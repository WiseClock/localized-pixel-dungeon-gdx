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

import java.nio.FloatBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.watabou.gdx.GdxTexture;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Matrix;
import com.watabou.glwrap.Quad;

import com.watabou.utils.RectF;
import net.whitegem.pixeldungeon.FontFactory;
import net.whitegem.pixeldungeon.LanguageFactory;
import net.whitegem.pixeldungeon.Translator;

public class BitmapText extends Visual {

	protected String text;
	protected String translatedText;
	protected Font font;

	protected float transWidth;
	protected float transHeight;
	protected String fontScale;

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
		this.font = font;


		translate();
	}

	private void translate()
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
			translatedText = (text == null) ? "" : (s == null) ? LanguageFactory.INSTANCE.translate(text) : s;
		}

		fontScale = "1x";
		if (font != null && font.baseLine != 0)
		{
			switch ((int) font.baseLine)
			{
				case 9:
					fontScale = "15x";
					//Game.batch.setShader(LanguageFactory.shaderPixel);
					break;
				case 11:
					fontScale = "2x";
					//Game.batch.setShader(LanguageFactory.shaderPixel);
					break;
				case 13:
					fontScale = "25x";
					//Game.batch.setShader(LanguageFactory.shaderPixel);
					break;
				case 17:
					fontScale = "3x";
					//Game.batch.setShader(LanguageFactory.shaderPixel);
					break;
				default:
					fontScale = "1x";
					//Game.batch.setShader(LanguageFactory.shaderPixel);
					break;
			}
		}

		if (translatedText != null)
		{
			measure();

			if (transWidth == 0)
			{
				if (this instanceof BitmapTextMultiline)
				{
					transWidth = width;
				}
				else
				{
					transWidth = LanguageFactory.INSTANCE.getFont(fontScale).getBounds(translatedText).width;
				}
			}

			if (transHeight == 0)
			{
				if (this instanceof BitmapTextMultiline)
				{
					transHeight = LanguageFactory.INSTANCE.getFont(fontScale).getWrappedBounds(translatedText, width).height;
				}
				else
				{
					transHeight = LanguageFactory.INSTANCE.getFont(fontScale).getBounds(translatedText).height;
				}
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
				ra, ga, ba, aa);

		if (translatedText == null)
		{
			script.drawQuadSet(quads, realLength);
		}
		else
		{
			String col = String.format("[#%02X%02X%02X%02X]", (int)Math.abs(rm * 255), (int)Math.abs(gm * 255), (int)Math.abs(bm * 255), (int)Math.abs(am * 255));
			String coloredTranslatedText = col + translatedText + "[]";

			Game.batch.begin();

			float zoom = camera().zoom;
			int camX = camera().x;
			int camY = camera().y;

			if (zoom < 4)
			{
				transWidth *= 2;
				transHeight *= 2;
				if (this instanceof BitmapTextMultiline)
				{
					// not working for GameLog
					// LanguageFactory.INSTANCE.getFont(fontScale).drawWrapped(Game.batch, translatedText, camX + (camera().screenWidth - width) / 2, Game.height - camY - (camera().screenHeight - height) / 2, width);
					LanguageFactory.INSTANCE.getFont(fontScale).drawWrapped(Game.batch, coloredTranslatedText, camX + x * zoom, Game.height - camY - y * zoom, width);
				}
				else
				{
					LanguageFactory.INSTANCE.getFont(fontScale).draw(Game.batch, coloredTranslatedText, camX + x * zoom, Game.height - (y * zoom + camY));
				}
			}
			else
			{
				if (this instanceof BitmapTextMultiline)
				{
					LanguageFactory.INSTANCE.getFont(fontScale).scale(camera().zoom / 2);
					// not working for GameLog
					// LanguageFactory.INSTANCE.getFont(fontScale).drawWrapped(Game.batch, translatedText, camX + (camera().screenWidth - width) / 2, Game.height - camY - (camera().screenHeight - height) / 2, width);
					LanguageFactory.INSTANCE.getFont(fontScale).drawWrapped(Game.batch, coloredTranslatedText, camX + x * zoom, Game.height - camY - y * zoom, width);
					LanguageFactory.INSTANCE.getFont(fontScale).setScale(1);
				}
				else
				{
					LanguageFactory.INSTANCE.getFont(fontScale).scale(camera().zoom / 2);
					LanguageFactory.INSTANCE.getFont(fontScale).draw(Game.batch, coloredTranslatedText, camX + x * zoom, Game.height - (y * zoom + camY));
					LanguageFactory.INSTANCE.getFont(fontScale).setScale(1);
				}
			}

			Game.batch.end();
			Game.batch.setShader(null);
			Gdx.gl.glEnable(GL20.GL_BLEND);
			NoosaScript.get().use();
		}
	}

	@Override
	public float width()
	{
		if (this instanceof BitmapTextMultiline)
			return super.width();
		if (transWidth != 0)
			return transWidth * scale.x;
		return super.width();
	}

	@Override
	public float height()
	{
		if (this instanceof BitmapTextMultiline)
			return super.height();
		if (transHeight != 0)
			return transHeight * scale.y;
		return super.height();
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
		translate();
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
			super( tx );
			
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
		
		public RectF get( char ch ) {
			return super.get( autoUppercase ? Character.toUpperCase( ch ) : ch );
		}
	}
}
