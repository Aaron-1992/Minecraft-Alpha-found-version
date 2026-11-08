import java.awt.image.*;

/** Native CPU voxel renderer. All surface pixels come from the original terrain atlas. */
final class VoxelRenderer {
    static final int WIDTH=320, HEIGHT=180;
    final BufferedImage frame=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
    final int[] pixels=((DataBufferInt)frame.getRaster().getDataBuffer()).getData();
    final int[] atlas;
    int distance=52;
    static final class Hit {
        int x,y,z,axis,normal,block;
        double t,u,v;
    }
    VoxelRenderer(BufferedImage texture) { atlas=texture.getRGB(0,0,256,256,null,0,256); }
    static int tile(int b,int axis,int normal) {
        return switch(b) {
            case 1 -> axis==1?(normal>0?0:2):3;
            case 2 -> 2; case 3 -> 1; case 4 -> axis==1?21:20;
            case 5 -> 52; case 6 -> 18; case 7 -> 4; case 8 -> 16;
            case 9 -> 49; case 10 -> 205; default -> 1;
        };
    }
    static boolean trace(VoxelWorld w,double ox,double oy,double oz,double dx,double dy,double dz,double max,boolean skipWater,Hit h) {
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
                h.x=x;h.y=y;h.z=z;h.axis=axis;h.normal=normal;h.block=b;h.t=t;
                h.u=axis==0?pz-Math.floor(pz):px-Math.floor(px);
                h.v=axis==1?pz-Math.floor(pz):1-(py-Math.floor(py));
                return true;
            }
            if(tx<ty && tx<tz){t=tx;tx+=ax;x+=sx;axis=0;normal=-sx;}
            else if(ty<tz){t=ty;ty+=ay;y+=sy;axis=1;normal=-sy;}
            else{t=tz;tz+=az;z+=sz;axis=2;normal=-sz;}
        }
        return false;
    }
    void render(VoxelWorld w,double seconds) {
        double sy=Math.sin(w.yaw),cy=Math.cos(w.yaw),sp=Math.sin(w.pitch),cp=Math.cos(w.pitch);
        double focal=1/Math.tan(Math.toRadians(70)/2),aspect=WIDTH/(double)HEIGHT;
        int fog=0xA4B8BE;
        double limit=distance+Math.sin(seconds*.012)*1.2;
        Hit h=new Hit();
        for(int yy=0;yy<HEIGHT;yy++) {
            double vy=(1-2*(yy+.5)/HEIGHT)/focal;
            for(int xx=0;xx<WIDTH;xx++) {
                double vx=(2*(xx+.5)/WIDTH-1)*aspect/focal;
                double dx,dy,dz;
                // Correct camera basis: up=(-sin(yaw)*sin(pitch), cos(pitch), -cos(yaw)*sin(pitch)).
                dx=vx*cy-sy*cp+vy*sy*sp;
                dy=sp+vy*cp;
                dz=-vx*sy-cy*cp+vy*cy*sp;
                double length=Math.sqrt(dx*dx+dy*dy+dz*dz);dx/=length;dy/=length;dz/=length;
                int rgb=fog;
                if(trace(w,w.x,w.y,w.z,dx,dy,dz,limit,false,h)) {
                    int tile=tile(h.block,h.axis,h.normal);
                    int u=Math.min(15,(int)(h.u*16)),v=Math.min(15,(int)(h.v*16));
                    int pixel=atlas[(tile/16*16+v)*256+tile%16*16+u];
                    // Cutout surfaces reveal the world behind them, using the original alpha channel.
                    if((pixel>>>24)<100 && h.block!=10) {
                        Hit behind=new Hit();
                        double exit=.002;
                        while(exit<3 && w.get((int)Math.floor(w.x+dx*(h.t+exit)),(int)Math.floor(w.y+dy*(h.t+exit)),(int)Math.floor(w.z+dz*(h.t+exit)))==h.block) exit+=.08;
                        double offset=h.t+exit;
                        if(trace(w,w.x+dx*offset,w.y+dy*offset,w.z+dz*offset,dx,dy,dz,Math.max(0,limit-offset),false,behind)) {
                            behind.t+=offset;h=behind;tile=tile(h.block,h.axis,h.normal);
                            u=Math.min(15,(int)(h.u*16));v=Math.min(15,(int)(h.v*16));
                            pixel=atlas[(tile/16*16+v)*256+tile%16*16+u];
                        } else pixel=0xff000000|fog;
                    }
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
}
