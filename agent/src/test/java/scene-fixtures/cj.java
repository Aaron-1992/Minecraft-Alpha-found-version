/** Flat test world: reading an unloaded chunk throws, so accidental generation paths fail tests. */
public class cj {
    public jx figure;
    public int spawned,reads;
    public boolean wall,trees;
    public int blockedZ=-999;
    private boolean h(int x,int z){return x>=-2&&x<=2&&z>=-2&&z<=2;}
    public int a(int x,int y,int z){if(!h(x>>4,z>>4))throw new AssertionError("Unloaded world read");reads++;if(trees&&x==-13&&z==18&&y>=64&&y<=66)return 17;return y<=63||((wall&&z==10||z==blockedZ)&&y<69)?1:0;}
    public boolean a(jx entity){figure=entity;spawned++;return true;}
}
