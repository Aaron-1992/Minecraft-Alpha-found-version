package alpha.ambient;

/** Copies the actual renderer's clipping planes; never performs OpenGL work on a tick. */
public final class AmbientHooksVisibility {
    private static volatile Frame frame;
    private static final class Frame {
        final float[][] planes;final double x,y,z;final long time;
        Frame(float[][] p,double x,double y,double z){planes=p;this.x=x;this.y=y;this.z=z;time=System.nanoTime();}
    }
    public static void capture(Object camera,double x,double y,double z){
        try{
            java.lang.reflect.Field field=camera.getClass().getDeclaredField("a");field.setAccessible(true);
            Object clipping=field.get(camera);float[][] planes=(float[][])clipping.getClass().getField("a").get(clipping);
            capturePlanes(planes,x,y,z);
        }catch(ReflectiveOperationException e){frame=null;}
    }
    static void capturePlanes(float[][] source,double x,double y,double z){
        if(source==null||source.length!=6){frame=null;return;}
        float[][] copy=new float[6][4];
        for(int i=0;i<6;i++){
            if(source[i]==null||source[i].length<4){frame=null;return;}
            double norm=0;
            for(int j=0;j<4;j++){float v=source[i][j];if(Float.isNaN(v)||Float.isInfinite(v)){frame=null;return;}copy[i][j]=v;if(j<3)norm+=v*v;}
            if(norm<.000001){frame=null;return;}
        }
        frame=new Frame(copy,x,y,z);
    }
    static void clear(){frame=null;}
    static boolean available(double x,double y,double z){Frame f=frame;return f!=null&&System.nanoTime()-f.time<250000000L&&Math.abs(f.x-x)<2&&Math.abs(f.y-y)<3&&Math.abs(f.z-z)<2;}
    /** Missing/stale matrices protect everything. World coordinates, generous half-block margin. */
    static boolean intersects(double x0,double y0,double z0,double x1,double y1,double z1){
        Frame f=frame;if(f==null||System.nanoTime()-f.time>250000000L)return true;
        for(float[] p:f.planes){
            double x=p[0]>=0?x1+.5:x0-.5,y=p[1]>=0?y1+.5:y0-.5,z=p[2]>=0?z1+.5:z0-.5;
            if(p[0]*(x-f.x)+p[1]*(y-f.y)+p[2]*(z-f.z)+p[3]<0)return false;
        }return true;
    }
    private AmbientHooksVisibility(){}
}
