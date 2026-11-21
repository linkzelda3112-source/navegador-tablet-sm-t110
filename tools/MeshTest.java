import com.valleradial.MeshBuilder;
public class MeshTest { 
 static void rep(String n, MeshBuilder m){ float[] d=m.toArray(); boolean nan=false,bad=false; float ymin=9e9f,ymax=-9e9f,wmax=0;
  for(int i=0;i<d.length;i+=10){ for(int k=0;k<10;k++) if(Float.isNaN(d[i+k])||Float.isInfinite(d[i+k])) nan=true; ymin=Math.min(ymin,d[i+1]); ymax=Math.max(ymax,d[i+1]); wmax=Math.max(wmax,d[i+9]);
   float l=(float)Math.sqrt(d[i+3]*d[i+3]+d[i+4]*d[i+4]+d[i+5]*d[i+5]); if(Math.abs(l-1)>0.02f) bad=true; }
  System.out.printf("%-14s tris=%5d nan=%b badNormal=%b y[%.1f,%.1f] swayMax=%.2f mod3=%d%n", n, d.length/10/3, nan, bad, ymin,ymax,wmax,(d.length/10)%3); }
 public static void main(String[] a){
  for(int v=0;v<4;v++){ rep("house"+v, MeshBuilder.house(v)); rep("houseSimple"+v, MeshBuilder.houseSimple(v)); rep("tree"+v, MeshBuilder.tree(v)); rep("treeSimple"+v, MeshBuilder.treeSimple(v)); }
  rep("rock",MeshBuilder.rock()); rep("rockSimple",MeshBuilder.rockSimple()); rep("bush0",MeshBuilder.bush(0)); rep("bush1",MeshBuilder.bush(1));
  rep("tower",MeshBuilder.tower()); rep("towerSimple",MeshBuilder.towerSimple()); rep("wmBody",MeshBuilder.windmillBody()); rep("wmBlades",MeshBuilder.windmillBlades()); rep("wmSimple",MeshBuilder.windmillSimple());
  rep("arch",MeshBuilder.arch()); rep("archSimple",MeshBuilder.archSimple()); rep("ruin",MeshBuilder.ruin()); rep("ruinSimple",MeshBuilder.ruinSimple()); rep("enemy",MeshBuilder.enemy());
  MeshBuilder g=new MeshBuilder(); java.util.Random r=new java.util.Random(1); for(int i=0;i<110;i++) g.tuft(0,-230,0,0,1,0,r); rep("tuftsx110",g);
 }}
