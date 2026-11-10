/** Flat test world: reading an unloaded chunk throws, so accidental generation paths fail tests. */
public class cj {
    public final java.util.List<Object> v=new java.util.ArrayList<Object>(),a=new java.util.ArrayList<Object>();
    public final java.util.Map<String,Integer> blocks=new java.util.HashMap<String,Integer>(),metadata=new java.util.HashMap<String,Integer>();
    public int writes,range=2,failWrite=-1,rayCalls;
    public boolean rayBlocked;
    public Object a(ah start,ah end){rayCalls++;return rayBlocked?new Object():null;}
    public long b=14000;
    private String key(int x,int y,int z){return x+","+y+","+z;}
    public void fixture(int x,int y,int z,int id){blocks.put(key(x,y,z),id);}
    public int e(int x,int y,int z){Integer n=metadata.get(key(x,y,z));return n==null?0:n;}
    public void b(int x,int y,int z,int value){metadata.put(key(x,y,z),value);writes++;}
    public boolean d(int x,int y,int z,int id){if(writes==failWrite){failWrite=-1;return false;}blocks.put(key(x,y,z),id);writes++;return true;}
    public boolean a(int x,int y,int z,int id){return d(x,y,z,id);}
    public void h(int x,int y,int z){}
    public jx figure;
    public int spawned,reads;
    public boolean wall,trees;
    public int blockedZ=-999;
    private boolean h(int x,int z){return x>=-range&&x<=range&&z>=-range&&z<=range;}
    public int a(int x,int y,int z){if(!h(x>>4,z>>4))throw new AssertionError("Unloaded world read");reads++;Integer value=blocks.get(key(x,y,z));if(value!=null)return value;if(trees&&x==2&&z==-21&&y>=64&&y<=66)return 17;return y<=63||((wall&&z==-10||z==blockedZ)&&y<69)?1:0;}
    public boolean a(jx entity){figure=entity;v.add(entity);spawned++;return true;}
}
