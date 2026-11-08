import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

final class VoxelWorld {
    static final int SIZE = 128, HEIGHT = 48, SEA = 13;
    static final String[] NAMES = {"Air", "Grass", "Dirt", "Stone", "Wood", "Leaves", "Sand", "Planks", "Cobblestone", "Glass", "Water"};
    final byte[] blocks = new byte[SIZE * SIZE * HEIGHT];
    final long seed;
    double x = 75.5, y = 27, z = 79.5, yaw = -.4, pitch = 0;
    int selected = 0;
    VoxelWorld(long seed) { this.seed = seed; generate(); spawn(); }
    private VoxelWorld(long seed, boolean unused) { this.seed = seed; }
    int get(int x, int y, int z) {
        if (x < 0 || z < 0 || x >= SIZE || z >= SIZE || y >= HEIGHT) return 0;
        if (y < 0) return 3;
        return blocks[(x * SIZE + z) * HEIGHT + y] & 255;
    }
    void set(int x, int y, int z, int b) {
        if (x >= 0 && z >= 0 && x < SIZE && z < SIZE && y >= 0 && y < HEIGHT)
            blocks[(x * SIZE + z) * HEIGHT + y] = (byte)b;
    }
    static boolean solid(int b) { return b != 0 && b != 10; }
    double hash(int x, int z) {
        long n = x * 374761393L + z * 668265263L + seed * 1447;
        n = (n ^ (n >>> 13)) * 1274126177L;
        return ((n ^ (n >>> 16)) & 0x7fffffffL) / (double)0x7fffffff;
    }
    double noise(double x, double z) {
        int a = (int)Math.floor(x), b = (int)Math.floor(z);
        double u = x-a, v = z-b; u *= u * (3-2*u); v *= v * (3-2*v);
        return lerp(lerp(hash(a,b),hash(a+1,b),u),lerp(hash(a,b+1),hash(a+1,b+1),u),v);
    }
    static double lerp(double a, double b, double t) { return a+(b-a)*t; }
    int height(int x, int z) {
        double depression = 12*Math.exp(-((x-64)*(x-64)+(z-59)*(z-59))/700.0);
        return (int)(10+noise(x/39.0,z/39.0)*13+noise(x/13.0,z/13.0)*5+noise(x/5.0,z/5.0)*1.7-depression);
    }
    private void generate() {
        for (int x=0;x<SIZE;x++) for (int z=0;z<SIZE;z++) {
            int h=height(x,z);
            for(int y=0;y<=Math.max(h,SEA);y++) set(x,y,z,y>h?10:y==h?(h<=SEA+1?6:1):y>h-3?2:3);
        }
        for(int x=4;x<SIZE-4;x++) for(int z=4;z<SIZE-4;z++) {
            int h=height(x,z);
            if(h>SEA+1 && hash(x*13,z*17)>.983 && noise(x/22.0,z/22.0)>.32) {
                int trunk=4+(int)(hash(x,z)*2);
                for(int y=h+1;y<=h+trunk;y++) set(x,y,z,4);
                for(int dy=-2;dy<=1;dy++) {
                    int r=dy==1?1:2;
                    for(int dx=-r;dx<=r;dx++) for(int dz=-r;dz<=r;dz++) {
                        if(Math.abs(dx)==r && Math.abs(dz)==r && hash(x+dx,z+dz)>.45) continue;
                        if(get(x+dx,h+trunk+dy,z+dz)==0) set(x+dx,h+trunk+dy,z+dz,5);
                    }
                }
            }
        }
    }
    private void spawn() {
        for(int xx=70;xx<100;xx++) for(int zz=75;zz<100;zz++) {
            int h=height(xx,zz);
            if(h>SEA+1 && get(xx,h+1,zz)==0 && get(xx,h+2,zz)==0) {
                x=xx+.5; y=h+2.63; z=zz+.5; return;
            }
        }
        y=HEIGHT+2;
    }
    boolean collides(double x, double y, double z) {
        for(int xx=(int)Math.floor(x-.28);xx<=(int)Math.floor(x+.28);xx++)
            for(int yy=(int)Math.floor(y-1.61);yy<=(int)Math.floor(y+.15);yy++)
                for(int zz=(int)Math.floor(z-.28);zz<=(int)Math.floor(z+.28);zz++)
                    if(solid(get(xx,yy,zz))) return true;
        return false;
    }
    void save(Path path) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temp=path.resolveSibling(path.getFileName()+".tmp");
        try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(temp)))) {
            out.writeInt(0x414C5048);out.writeInt(1);out.writeLong(seed);
            out.writeDouble(x);out.writeDouble(y);out.writeDouble(z);out.writeDouble(yaw);out.writeDouble(pitch);
            out.writeInt(selected);out.writeInt(blocks.length);out.write(blocks);
        }
        try { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
        catch(AtomicMoveNotSupportedException e) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING); }
    }
    static VoxelWorld load(Path path) throws IOException {
        try(DataInputStream in=new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))) {
            if(in.readInt()!=0x414C5048 || in.readInt()!=1) throw new IOException("Unsupported world file");
            VoxelWorld w=new VoxelWorld(in.readLong(),false);
            w.x=in.readDouble();w.y=in.readDouble();w.z=in.readDouble();w.yaw=in.readDouble();w.pitch=in.readDouble();
            w.selected=in.readInt();
            if(in.readInt()!=w.blocks.length) throw new IOException("Invalid world dimensions");
            in.readFully(w.blocks);
            if(!Double.isFinite(w.x+w.y+w.z+w.yaw+w.pitch) || w.x<1 || w.x>=SIZE-1 || w.z<1 || w.z>=SIZE-1 || w.y<0 || w.y>HEIGHT+4 || w.selected<0 || w.selected>8)
                throw new IOException("Invalid player data");
            for(byte b:w.blocks) if(b<0 || b>10) throw new IOException("Invalid block data");
            return w;
        }
    }
}
