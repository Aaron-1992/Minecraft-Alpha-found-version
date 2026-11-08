import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/** UI geometry uses only the user's original font and terrain pixels. */
final class AlphaPresentation {
    private final BufferedImage font,terrain;
    private final int[] widths=new int[256];
    AlphaPresentation(BufferedImage font,BufferedImage terrain){
        this.font=font;this.terrain=terrain;
        for(int c=0;c<256;c++){
            int right=-1;
            for(int x=0;x<8;x++)for(int y=0;y<8;y++){
                int p=font.getRGB(c%16*8+x,c/16*8+y);
                if((p>>>24)>32&&(p&0xffffff)!=0)right=Math.max(right,x);
            }
            widths[c]=c==32?4:Math.max(2,right+2);
        }
    }
    int width(String text){int width=0;for(int i=0;i<text.length();i++)width+=widths[glyph(text.charAt(i))];return width;}
    private static int glyph(char c){return c>255?'?':c;}
    void text(Graphics2D g,String text,int x,int y){
        for(int i=0;i<text.length();i++){
            int c=glyph(text.charAt(i)),sx=c%16*8,sy=c/16*8;
            g.drawImage(font,x,y,x+8,y+8,sx,sy,sx+8,sy+8,null);x+=widths[c];
        }
    }
    void block(Graphics2D g,int block,double x,double y,double size){
        // Project three original-texture faces, rather than flat texture swatches.
        face(g,VoxelRenderer.tile(block,1,1),x,y-size*.46,size*.5,-size*.25,size*.5,size*.25,1);
        face(g,VoxelRenderer.tile(block,0,-1),x,y-size*.46,size*.5,size*.25,0,size*.58,.6f);
        face(g,VoxelRenderer.tile(block,2,1),x+size*.5,y-size*.21,size*.5,-size*.25,0,size*.58,.8f);
    }
    private void face(Graphics2D g,int tile,double x,double y,double ax,double ay,double bx,double by,float shade){
        BufferedImage original=terrain.getSubimage(tile%16*16,tile/16*16,16,16);
        AffineTransform transform=new AffineTransform(ax/16,ay/16,bx/16,by/16,x,y);
        g.drawImage(original,transform,null);
        if(shade<1){
            Polygon polygon=new Polygon(new int[]{(int)x,(int)(x+ax),(int)(x+ax+bx),(int)(x+bx)},
                    new int[]{(int)y,(int)(y+ay),(int)(y+ay+by),(int)(y+by)},4);
            g.setColor(new Color(0,0,0,1-shade));g.fillPolygon(polygon);
        }
    }
    void title(Graphics2D g,BufferedImage logo){
        if(logo==null){Graphics2D text=(Graphics2D)g.create();text.scale(3,3);text(text,"Minecraft",80-width("Minecraft")/2,15);text.dispose();return;}
        if(logo.getWidth()==256&&logo.getHeight()>=89){
            g.drawImage(logo,103,35,258,79,0,0,155,44,null);
            g.drawImage(logo,258,35,377,79,0,45,119,89,null);
        }else{
            int h=Math.min(65,(int)(274.0*logo.getHeight()/logo.getWidth()));
            g.drawImage(logo,103,35,274,h,null);
        }
    }
}
