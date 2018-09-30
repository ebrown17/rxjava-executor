import java.util.ArrayDeque;
import java.util.ArrayList;

public class IdGenerator {

  private String name;
  private ArrayDeque<Integer> identityPool;
  private ArrayList<Integer> identityInUse;
  private static final int POOL_INCREMENT_SIZE = 1000;
  private int lowestUnassignedID = 1;

  public IdGenerator(String name) {
    this.name = name;
    identityPool = new ArrayDeque<Integer>(POOL_INCREMENT_SIZE);
    identityInUse = new ArrayList<Integer>();
    fillIdPool();
  }


  public Integer getNewId() {
    if (!identityPool.isEmpty()) {
      Integer id = identityPool.pop();
      identityInUse.add(id);
      return id;
    }
    else {
      fillIdPool();
      if (!identityPool.isEmpty()) {
        Integer id = identityPool.pop();
        identityInUse.add(id);
        return id;
      }
      throw new Error("No IDs left");
    }

  }
  
  public void recycleId(Integer id) {
    if(identityInUse.contains(id)) {
      identityInUse.remove(id);
      identityPool.add(id);
    }
  }

  private void fillIdPool() {
    for (int i = 0; i < POOL_INCREMENT_SIZE; i++) {
      if (lowestUnassignedID < Integer.MAX_VALUE) {
        identityPool.add(lowestUnassignedID++);
      }
      else {
        for (int j = 1; j < Integer.MAX_VALUE; j++) {
          if (!identityPool.contains(j) && !identityInUse.contains(j)) {
            identityPool.add(j);
            return;
          }
        }
        throw new Error("Exception: No IDs are available.");
      }
    }
  }

  @Override
  public String toString() {
    return "Identity [name=" + name + ", identityPool=" + identityPool.size() + ", identityInUse=" + identityInUse.size()
        + ", lowestUnassignedID=" + lowestUnassignedID + "]";
  }
  
  public Integer getIdentityPoolSize() {
    return identityPool.size();
  }
  
  public Integer getIdentitiesInUseSize() {
    return identityInUse.size();
  }
  
  public static void main(String[] args) {
    IdGenerator test = new IdGenerator("Test");
    System.out.println(test.toString());
    for(int i=0;i<1000; i++) {
      test.getNewId();
    }
    System.out.println(test.toString());
    for(int i=0;i<1000; i++) {
      test.getNewId();
    }
    System.out.println(test.toString());
    Integer h =test.getNewId();
    System.out.println(test.toString());
    test.recycleId(h);
    System.out.println(test.toString());
    test.getNewId();
    test.getNewId();
  }

}
