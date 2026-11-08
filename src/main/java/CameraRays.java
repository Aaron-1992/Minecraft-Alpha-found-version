/** Projection math only, precomputed once instead of normalizing every pixel each frame. */
final class CameraRays {
    final double[] right,up,forward;
    CameraRays(int width,int height,double fieldOfView){
        if(width<1||height<1||fieldOfView<=0||fieldOfView>=180)throw new IllegalArgumentException("Invalid projection");
        right=new double[width*height];up=new double[right.length];forward=new double[right.length];
        double tangent=Math.tan(Math.toRadians(fieldOfView)/2),aspect=width/(double)height;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            double r=(2*(x+.5)/width-1)*aspect*tangent,u=(1-2*(y+.5)/height)*tangent;
            double length=Math.sqrt(1+r*r+u*u);int i=y*width+x;
            right[i]=r/length;up[i]=u/length;forward[i]=1/length;
        }
    }
}
