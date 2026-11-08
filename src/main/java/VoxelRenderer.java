import java.awt.image.*;

/** Native CPU voxel renderer. All surface pixels come from the original terrain atlas. */
final class VoxelRenderer {
    private final CameraRays rays=new CameraRays(WIDTH,HEIGHT,70);
    static final int WIDTH=480, HEIGHT=270;
    final BufferedImage frame=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
    final int[] pixels=((DataBufferInt)frame.getRaster().getDataBuffer()).getData();
    final int[] atlas;
    private int[] cloudPixels;
    private int cloudWidth,cloudHeight;
    int distance=52;
    static final class Hit {
        int x,y,z,axis,normal,block,pixel;
        double t,u,v;
    }
    VoxelRenderer(BufferedImage texture) { atlas=texture.getRGB(0,0,256,256,null,0,256); }
    void setClouds(BufferedImage original){
        if(original==null){cloudPixels=null;return;}
        cloudWidth=original.getWidth();cloudHeight=original.getHeight();
        cloudPixels=original.getRGB(0,0,cloudWidth,cloudHeight,null,0,cloudWidth);
    }
    static int tile(int b,int axis,int normal) {
        return switch(b) {
            case 1 -> axis==1?(normal>0?0:2):3;
            case 2 -> 2; case 3 -> 1; case 4 -> axis==1?21:20;
            case 5 -> 52; case 6 -> 18; case 7 -> 4; case 8 -> 16;
            case 9 -> 49; case 10 -> 205; default -> 1;
        };
    }
    static boolean trace(VoxelWorld w,double ox,double oy,double oz,double dx,double dy,double dz,double max,boolean skipWater,Hit h) {
        return traceSurface(w,ox,oy,oz,dx,dy,dz,max,skipWater,null,h);
    }
    private static boolean traceSurface(VoxelWorld w,double ox,double oy,double oz,double dx,double dy,double dz,double max,boolean skipWater,int[] atlas,Hit h){
        int x=(int)Math.floor(ox),y=(int)Math.floor(oy),z=(int)Math.floor(oz);
        int sx=dx>=0?1:-1,sy=dy>=0?1:-1,sz=dz>=0?1:-1;
        double ax=dx==0?Double.POSITIVE_INFINITY:Math.abs(1/dx),ay=dy==0?Double.POSITIVE_INFINITY:Math.abs(1/dy),az=dz==0?Double.POSITIVE_INFINITY:Math.abs(1/dz);
        double tx=dx==0?Double.POSITIVE_INFINITY:(dx>0?x+1-ox:ox-x)*ax;
        double ty=dy==0?Double.POSITIVE_INFINITY:(dy>0?y+1-oy:oy-y)*ay;
        double tz=dz==0?Double.POSITIVE_INFINITY:(dz>0?z+1-oz:oz-z)*az;
        double t=0;int axis=1,normal=1;
        while(t<=max) {
            int b=w.get(x,y,z);
            if(b!=0 && !(skipWater && b==10)) {
                double px=ox+dx*(t+.000001),py=oy+dy*(t+.000001),pz=oz+dz*(t+.000001);
                double u=axis==0?pz-Math.floor(pz):px-Math.floor(px);
                double v=axis==1?pz-Math.floor(pz):1-(py-Math.floor(py));
                int pixel=0;
                if(atlas!=null){
                    int tile=tile(b,axis,normal);
                    int tu=Math.min(15,(int)(u*16)),tv=Math.min(15,(int)(v*16));
                    pixel=atlas[(tile/16*16+tv)*256+tile%16*16+tu];
                }
                if(atlas==null||(pixel>>>24)>=100||b==10){
                    h.x=x;h.y=y;h.z=z;h.axis=axis;h.normal=normal;h.block=b;h.t=t;
                    h.u=u;h.v=v;h.pixel=pixel;return true;
                }
                // Transparent texels continue the SAME grid walk: no allocations,
                // no approximate .08-unit marching and no skipping whole trees.
            }
            if(tx<ty && tx<tz){t=tx;tx+=ax;x+=sx;axis=0;normal=-sx;}
            else if(ty<tz){t=ty;ty+=ay;y+=sy;axis=1;normal=-sy;}
            else{t=tz;tz+=az;z+=sz;axis=2;normal=-sz;}
        }
        return false;
    }
    void render(VoxelWorld w,double seconds) {
        double sy=Math.sin(w.yaw),cy=Math.cos(w.yaw),sp=Math.sin(w.pitch),cp=Math.cos(w.pitch);
        int fog=0xB8CFE5;
        double limit=distance+Math.sin(seconds*.012)*1.2;
        Hit h=new Hit();
        for(int yy=0;yy<HEIGHT;yy++) {
            for(int xx=0;xx<WIDTH;xx++) {
                int ray=yy*WIDTH+xx;
                double right=rays.right[ray],up=rays.up[ray],forward=rays.forward[ray];
                double dx=right*cy-sy*cp*forward+up*sy*sp;
                double dy=sp*forward+up*cp;
                double dz=-right*sy-cy*cp*forward+up*cy*sp;
                double horizon=Math.max(0,Math.min(1,dy*1.8));
                int rgb=blend(fog,0x78A7FF,horizon);
                if(cloudPixels!=null && dy>.005){
                    double t=(42-w.y)/dy;
                    if(t>0 && t<260){
                        int cx=Math.floorMod((int)Math.floor((w.x+dx*t+seconds*.35)/12),cloudWidth);
                        int cz=Math.floorMod((int)Math.floor((w.z+dz*t)/12),cloudHeight);
                        int cloud=cloudPixels[cz*cloudWidth+cx];
                        if((cloud>>>24)>100 && (cloud&0xffffff)>0x808080)
                            rgb=blend(rgb,cloud,Math.max(0,1-t/300));
                    }
                }
                if(traceSurface(w,w.x,w.y,w.z,dx,dy,dz,limit,false,atlas,h)) {
                    int pixel=h.pixel;
                    double shade=h.axis==1?(h.normal>0?1:.5):h.axis==0?.7:.83;
                    double fade=Math.max(0,Math.min(1,(h.t-limit*.36)/(limit*.64)));
                    int r=(int)(((pixel>>16)&255)*shade),g=(int)(((pixel>>8)&255)*shade),b=(int)((pixel&255)*shade);
                    r=(int)(r*(1-fade)+((fog>>16)&255)*fade);g=(int)(g*(1-fade)+((fog>>8)&255)*fade);b=(int)(b*(1-fade)+(fog&255)*fade);
                    rgb=(r<<16)|(g<<8)|b;
                }
                pixels[yy*WIDTH+xx]=rgb;
            }
        }
    }
    private static int blend(int a,int b,double t){
        int r=(int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t);
        int g=(int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t);
        int blue=(int)((a&255)*(1-t)+(b&255)*t);
        return (r<<16)|(g<<8)|blue;
    }

}
