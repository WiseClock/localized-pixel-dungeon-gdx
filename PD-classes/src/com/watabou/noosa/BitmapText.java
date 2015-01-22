/*
 * Copyright (C) 2012-2014  Oleg Dolya
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.watabou.noosa;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.watabou.gdx.GdxTexture;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Matrix;
import com.watabou.glwrap.Quad;

import com.watabou.utils.RectF;
import net.whitegem.pixeldungeon.LanguageFactory;

public class BitmapText extends Visual {

	protected String text;
	protected Font font;

	protected float[] vertices = new float[16];
	protected FloatBuffer quads;
	
	public int realLength;
	
	protected boolean dirty = true;
	
	public BitmapText() {
		this( "", null );
	}
	
	public BitmapText( Font font ) {
		this( "", font );
	}
	
	public BitmapText( String text, Font font ) {
		super( 0, 0, 0, 0 );
		
		this.text = text;
		setTranslation();
		this.font = font;
	}

	private void setTranslation()
	{
		if (!LanguageFactory.INSTANCE.language.equals("en"))
		{
			String s = null;
			if (text != null && LanguageFactory.INSTANCE.stored.containsKey(text.toLowerCase().replaceAll("\\.$", "")))
			{
				// todo: search combined
				s = LanguageFactory.INSTANCE.stored.get(text.toLowerCase().replaceAll("\\.$", ""));
				LanguageFactory.INSTANCE.stored.remove(text.toLowerCase().replaceAll("\\.$", ""));
			}
			if (text != null && (LanguageFactory.INSTANCE.hasKey(text) || s != null))
			{
				text = (text == null) ? "" : (s == null) ? LanguageFactory.INSTANCE.translate(text) : s;
			}
		}
	}
	
	@Override
	public void destroy() {
		text = null;
		font = null;
		vertices = null;
		quads = null;
		super.destroy();
	}
	
	@Override
	protected void updateMatrix() {
		// "origin" field is ignored
		Matrix.setIdentity( matrix );
		Matrix.translate( matrix, x, y );
		Matrix.scale( matrix, scale.x, scale.y );
		Matrix.rotate( matrix, angle );
	}
	
	@Override
	public void draw() {
		
		super.draw();
		
		NoosaScript script = NoosaScript.get();
		
		font.texture.bind();
		
		if (dirty) {
			updateVertices();
		}
		
		script.camera( camera() );
		
		script.uModel.valueM4( matrix );
		script.lighting( 
			rm, gm, bm, am, 
			ra, ga, ba, aa );
		script.drawQuadSet( quads, realLength );
		
	}
	
	protected void updateVertices() {
		
		width = 0;
		height = 0;
		
		if (text == null) {
			text = "";
		}
		
		quads = Quad.createSet( text.length() );
		realLength = 0;
		
		int length = text.length();
		for (int i=0; i < length; i++) {
			RectF rect = font.get( text.charAt( i ) );
	
			if (rect == null) {
				rect=null;
			}
			float w = font.width( rect );
			float h = font.height( rect );
			
			vertices[0] 	= width;
			vertices[1] 	= 0;
			
			vertices[2]		= rect.left;
			vertices[3]		= rect.top;
			
			vertices[4] 	= width + w;
			vertices[5] 	= 0;
			
			vertices[6]		= rect.right;
			vertices[7]		= rect.top;
			
			vertices[8] 	= width + w;
			vertices[9] 	= h;
			
			vertices[10]	= rect.right;
			vertices[11]	= rect.bottom;
			
			vertices[12]	= width;
			vertices[13]	= h;
			
			vertices[14]	= rect.left;
			vertices[15]	= rect.bottom;
			
			quads.put( vertices );
			realLength++;
			
			width += w + font.tracking;
			if (h > height) {
				height = h;
			}
		}
		
		if (length > 0) {
			width -= font.tracking;
		}
		
		dirty = false;
		
	}
	
	public void measure() {
		
		width = 0;
		height = 0;
		
		if (text == null) {
			text = "";
		}
		
		int length = text.length();
		for (int i=0; i < length; i++) {
			RectF rect = font.get( text.charAt( i ) );
	
			float w = font.width( rect );
			float h = font.height( rect );
			
			width += w + font.tracking;
			if (h > height) {
				height = h;
			}
		}
		
		if (length > 0) {
			width -= font.tracking;
		}
	}
	
	public float baseLine() {
		return font.baseLine * scale.y;
	}
	
	public Font font() {
		return font;
	}
	
	public void font( Font value ) {
		font = value;
	}
	
	public String text() {
		return text;
	}
	
	public void text( String str ) {
		text = str;
		setTranslation();
		dirty = true;
	}
	
	public static class Font extends TextureFilm {
		
		public static final String LATIN_UPPER = 
			" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		
		public static final String LATIN_FULL = 
			" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007F";
		
		public SmartTexture texture;
		
		public float tracking = 0;
		public float baseLine;
		
		public boolean autoUppercase = false;
		
		public float lineHeight;
		
		protected Font( SmartTexture tx ) {
			super( tx );
			
			texture = tx;
		}
		
		public Font( SmartTexture tx, int width, String chars ) {
			this( tx, width, tx.height, chars );
		}
		
		public Font( SmartTexture tx, int width, int height, String chars ) {
			super(tx);
			
			texture = tx;
			
			autoUppercase = chars.equals( LATIN_UPPER );
			
			int length = chars.length();
			
			float uw = (float)width / tx.width;
			float vh = (float)height / tx.height;
			
			float left = 0;
			float top = 0;
			float bottom = vh;
			
			for (int i=0; i < length; i++) {
				RectF rect = new RectF( left, top, left += uw, bottom );
				add( chars.charAt( i ), rect );
				if (left >= 1) {
					left = 0;
					top = bottom;
					bottom += vh;
				}
			}
			
			lineHeight = baseLine = height;
		}

		private class FntFileChar
		{
			protected char c;
			protected int x, y, width, height, x_offset, y_offset;
			public FntFileChar(char c, int x, int y, int width, int height, int x_offset, int y_offset)
			{
				this.c=c;
				this.x=x;
				this.y=y;
				this.width=width;
				this.height=height;
				this.x_offset=x_offset;
				this.y_offset=y_offset;
			}
		}

		private ArrayList<FntFileChar> processFntFile(String fntFile)
		{
			ArrayList<FntFileChar> chars = new ArrayList<FntFileChar>();

			FileHandle file = Gdx.files.internal(fntFile);
			BufferedReader reader = new BufferedReader(file.reader());
			ArrayList<String> lines = new ArrayList<String>();
			int start = 0;
			try
			{
				String line = reader.readLine();
				while (line != null)
				{
					start++;
					if (!line.trim().equals("") && start >= 5)
					{
						lines.add(line.trim());
					}
					line = reader.readLine();
				}
			} catch (IOException ioe)
			{
			}
			for (String line : lines)
			{
				String[] objs = line.split("\\s+");
				String id = objs[1].split("=")[1];
				String x = objs[2].split("=")[1];
				String y = objs[3].split("=")[1];
				String w = objs[4].split("=")[1];
				String h = objs[5].split("=")[1];
				String xo = objs[6].split("=")[1];
				String yo = objs[7].split("=")[1];
				int idi = Integer.parseInt(id);
				int xi = Integer.parseInt(x);
				int yi = Integer.parseInt(y);
				int wi = Integer.parseInt(w);
				int hi = Integer.parseInt(h);
				int xoi = Integer.parseInt(xo);
				int yoi = Integer.parseInt(yo);
				char idc = (char)idi;
				FntFileChar ffc = new FntFileChar(idc, xi, yi, wi, hi, xoi, yoi);
				chars.add(ffc);
			}
			return chars;
		}

		protected void splitBy( GdxTexture bitmap, int height, int color, String chars, String fntFile ) {
			autoUppercase = false;

			ArrayList<FntFileChar> realChars = processFntFile(fntFile);

			int width = bitmap.getWidth();
			int length = realChars.size();

			TextureData td = bitmap.getTextureData();
			if (!td.isPrepared()) {
				td.prepare();
			}
			final Pixmap pixmap = td.consumePixmap();

			for (int i=0; i < length; i++) {
				FntFileChar ch = realChars.get(i);
				if (ch.c == ' ')
				{
					add(ch.c, new RectF(1 - (float)ch.height / 2 / width, 1 - (float)ch.height / height, 1, 1));
				}
				else
				{
					add(ch.c, new RectF((float) ch.x / width, (float) ch.y / height, (float) (ch.x + ch.width) / width, (float) (ch.y + ch.height) / height));
				}
			}
			pixmap.dispose();

			lineHeight = baseLine = height( frames.get( realChars.get( 0 ).c ) );
		}
		
		protected void splitBy( GdxTexture bitmap, int height, int color, String chars ) {
			
			autoUppercase = chars.equals( LATIN_UPPER );
			int length = chars.length();
			
			int width = bitmap.getWidth();
			float vHeight = (float)height / bitmap.getHeight();
			
			int pos;


			TextureData td = bitmap.getTextureData();
			if (!td.isPrepared()) {
				td.prepare();
			}
			final Pixmap pixmap = td.consumePixmap();
		spaceMeasuring:
			for (pos=0; pos <  width; pos++) {
				for (int j=0; j < height; j++) {
					if (colorNotMatch(pixmap, pos, j, color)) break spaceMeasuring;
				}
			}
			add( ' ', new RectF( 0, 0, (float)pos / width, vHeight ) );
			
			for (int i=0; i < length; i++) {
				
				char ch = chars.charAt( i );
				if (ch == ' ') {
					continue;
				} else {
					
					boolean found;
					int separator = pos;
					
					do {
						if (++separator >= width) {
							break;
						}
						found = true;
						for (int j=0; j < height; j++) {
							if (colorNotMatch(pixmap, separator, j, color)) {
								found = false;
								break;
							}
						}
					} while (!found);
					add( ch, new RectF( (float)pos / width, 0, (float)separator / width, vHeight ) );
					pos = separator + 1;
				}
			}
			pixmap.dispose();
			
			lineHeight = baseLine = height( frames.get( chars.charAt( 0 ) ) );
		}

		private boolean colorNotMatch(Pixmap pixmap, int x, int y, int color) {
			int pixel = pixmap.getPixel(x, y);
			if ((pixel & 0xFF) == 0) {
				return color != 0;
			}
			return pixel != color;
		}

		public static Font colorMarked( GdxTexture bmp, int color, String chars ) {
			Font font = new Font( TextureCache.get( bmp ) );
			font.splitBy( bmp, bmp.getHeight(), color, chars );
			return font;
		}

		public static Font colorMarked( GdxTexture bmp, int height, int color, String chars ) {
			Font font = new Font( TextureCache.get( bmp ) );
			font.splitBy( bmp, height, color, chars );
			return font;
		}

		public static Font colorMarked( GdxTexture bmp, int color, String chars, String fntFile ) {
			Font font = new Font( TextureCache.get( bmp ) );
			font.splitBy( bmp, bmp.getHeight(), color, chars, fntFile );
			return font;
		}

		public static Font colorMarked( GdxTexture bmp, int height, int color, String chars, String fntFile ) {
			Font font = new Font( TextureCache.get( bmp ) );
			font.splitBy( bmp, height, color, chars, fntFile );
			return font;
		}
		
		public RectF get( char ch ) {
			return super.get( autoUppercase ? Character.toUpperCase( ch ) : ch );
		}
	}
}
