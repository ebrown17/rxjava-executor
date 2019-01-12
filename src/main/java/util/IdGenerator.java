package util;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class IdGenerator {

  private String name;
  private ArrayDeque<Integer> identityPool;
  private ArrayList<Integer> identityInUse;
  private final int POOL_INCREMENT_SIZE;
  private final int MAX_ID;
  private int lowestUnassignedID = 1;

  /**
   * IdGenerator generates unique id's. Id's are kept in an Integer pool. The pool has an initial
   * size of 1000 and when this is reached; it increments by 1000. This happens until the max pool
   * size is reached which by default 50K. Both the pool increment size and the max size can be
   * overridden.
   *
   * @param name to be used for this generator.
   * @param poolIncSize the amount to increase the id pool by when pool is filled. Setting <= 0 will
   *     mean use default of 1000. If this is set, it must be <= half of maxSize and maxSize must
   *     also not be 0; or default values will be used.
   * @param maxSize of the id pool for this generator. Setting <= 0 will mean use default of 50K. If
   *     set, it must be >= 2 times poolIncSize and poolIncSize must not be 0; or default values
   *     will be used.
   */
  public IdGenerator(String name, int poolIncSize, int maxSize) {
    this.name = name;

    if (maxSize <= 0 || poolIncSize <= 0) {
      POOL_INCREMENT_SIZE = 1000;
      MAX_ID = 50000;
    } else {
      if ((poolIncSize * 2) > maxSize) {
        POOL_INCREMENT_SIZE = 1000;
        MAX_ID = 50000;
      } else {
        POOL_INCREMENT_SIZE = poolIncSize;
        MAX_ID = maxSize;
      }
    }
    identityPool = new ArrayDeque<Integer>(POOL_INCREMENT_SIZE);
    identityInUse = new ArrayList<Integer>();
    for (int i = 0; i < POOL_INCREMENT_SIZE; i++) {
      if (lowestUnassignedID < MAX_ID) {
        identityPool.add(lowestUnassignedID++);
      }
    }
  }

  /**
   * @return Int id of next available identity
   * @throws Exception thrown when no id's available.
   */
  public synchronized Integer getNewId() throws Exception {
    if (!identityPool.isEmpty()) {
      Integer id = identityPool.pop();
      identityInUse.add(id);
      return id;
    } else {
      fillIdPool();
      if (!identityPool.isEmpty()) {
        Integer id = identityPool.pop();
        identityInUse.add(id);
        return id;
      }
    }
    throw new Exception(
        "Exception: No IDs are available. In Use: "
            + identityInUse.size()
            + " Pool Size: "
            + identityPool.size()
            + " Max: "
            + MAX_ID);
  }

  /**
   * @param id to be returned to the identity pool
   */
  public synchronized void recycleId(Integer id) {
    if (identityInUse.contains(id)) {
      identityInUse.remove(id);
      identityPool.add(id);
    }
  }

  private void fillIdPool() throws Exception {
    for (int i = 0; i < POOL_INCREMENT_SIZE; i++) {
      if (lowestUnassignedID <= MAX_ID) {
        identityPool.add(lowestUnassignedID++);
        if (lowestUnassignedID > MAX_ID) {
          return;
        }
      }
      else {
        if (identityInUse.size() + identityPool.size() > MAX_ID) {
          throw new Exception(
              "Exception: No IDs are available. In Use: "
                  + identityInUse.size()
                  + " Pool Size: "
                  + identityPool.size()
                  + " Max: "
                  + MAX_ID
                  + " lowest unassigned "
                  + lowestUnassignedID);
        }
        else {
          for (int j = 1; j <= MAX_ID; j++) {
            if (!identityPool.contains(j) && !identityInUse.contains(j)) {
              identityPool.add(j);
              return;
            }
          }
          throw new Exception(
              "Exception: No IDs are available. In Use: "
                  + identityInUse.size()
                  + " Pool Size: "
                  + identityPool.size()
                  + " Max: "
                  + MAX_ID
                  + " lowest unassigned "
                  + lowestUnassignedID);
        }
      }
    }
  }

  @Override
  public String toString() {
    return "Identity [name="
        + name
        + ", identityPool="
        + identityPool.size()
        + ", identityInUse="
        + identityInUse.size()
        + ", lowestUnassignedID="
        + lowestUnassignedID
        + "]";
  }

  public Integer getIdentityPoolSize() {
    return identityPool.size();
  }

  public Integer getIdentitiesInUseSize() {
    return identityInUse.size();
  }
}
