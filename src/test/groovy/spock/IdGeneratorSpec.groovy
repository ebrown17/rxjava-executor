package spock

import spock.lang.Shared
import spock.lang.Specification
import util.IdGenerator

class IdGeneratorSpec extends Specification {

    def "Created with expected initial values"() {
        expect:
        def generator = new IdGenerator("SpockGenerator", incrSize, maxPoolSize)

        //println "[Pool Increment size: ${ incrSize } Max Pool Size: ${maxPoolSize}]"
        //println " increment > expected: ${expectInc} actual: ${generator.POOL_INCREMENT_SIZE}"
        //println " pool size > expected: ${expectMax} actual: ${generator.MAX_ID}"

        generator.POOL_INCREMENT_SIZE == expectInc
        generator.identityPoolSize == expectInc
        generator.identitiesInUseSize == 0
        generator.MAX_ID == expectMax

        where:
        incrSize | maxPoolSize | expectInc | expectMax
        1        | 1           | 1000      | 50000
        -1       | -1          | 1000      | 50000
        1        | -1          | 1000      | 50000
        -1       | 1           | 1000      | 50000
        -1001    | -1001       | 1000      | 50000
        1001     | -1001       | 1000      | 50000
        -1001    | 1001        | 1000      | 50000
        0        | 100000      | 1000      | 50000
        10000    | 0           | 1000      | 50000
        5        | 8           | 1000      | 50000
        100    | 150     | 1000   | 50000
        1001   | 2000    | 1000   | 50000
        1000   | 2000    | 1000   | 2000
        101    | 200     | 1000   | 50000
        1      | 200     | 1      | 200
        10     | 1000    | 10     | 1000
        2000   | 4000    | 2000   | 4000
        2001   | 8000    | 2001   | 8000
        100    | 10_000  | 100    | 10_000
        25_000 | 49_000  | 1000   | 50_000
        25_000 | 75_000  | 25_000 | 75_000
        5000   | 100_000 | 5000   | 100_000
        233    | 500     | 233    | 500
        233    | 466     | 233    | 466
        233    | 465     | 1000   | 50_000
        233    | 467     | 233    | 467
        1000   | 50_000  | 1000   | 50_000
        1001   | 50_000  | 1001   | 50_000
        1000   | 50_001  | 1000   | 50_001
        1001   | 50_001  | 1001   | 50_001
        333    | 1000    | 333    | 1000
        333    | 999     | 333    | 999
    }

    def "Generator gives and tracks the correct amount of IDs"() {
        expect:
        def generator = new IdGenerator("SpockGenerator", 0, 0)
        def count = 0
        idsWanted.times {
            if (generator.getNewId()) {
                count++
            }
        }

        count == idsWanted
        count == generator.getIdentitiesInUseSize()
        generator.getIdentityPoolSize() == expectedRemaining

        where:
        idsWanted | expectedRemaining
        1         | 999
        0         | 1000
        101       | 899
        500       | 500
        999       | 1
        1000      | 0
        1001      | 999
        25_000    | 0
        49_999    | 1
        50_000    | 0
        333       | 667
        1337      | 663
        12_011    | 989


    }

    def "Generator throws expected exception when it reaches max size"() {
        when:
        def generator = new IdGenerator("SpockGenerator", incrSize, maxSize)

        overMax.times {
            generator.getNewId()
        }
        then:
        Exception e = thrown()
        e.message.contains("Exception: No IDs are available.")

        where:
        incrSize | maxSize | overMax
        1000     | 2000    | 2001
        1        | 10      | 11
        1001     | 2000    | 50_001
        999      | 10_000  | 10_001
        5000     | 33_001  | 33_002
        1337     | 13_337  | 13_338
        3333     | 9991    | 9992
        0        | 0       | 50_001
    }

    def "Generator has expected pool values after IDs are recycled"() {
        when:
        def generator = new IdGenerator("SpockGenerator", incrSize, maxSize)

        def id
        def idMap = new HashMap<Integer, Integer>()

        idsGot.times {
            id = generator.getNewId()
            if (id) {
                idMap.put(id, id)
            }
        }

        def idsInUse = generator.getIdentitiesInUseSize()
        def poolSize = generator.getIdentityPoolSize()
        then:
        def mappedIds = []
        idsToRecycle.each {
            mappedIds << idMap.get(it)
        }

        def prePool = new ArrayDeque<Integer>(generator.identityPool)

        mappedIds.each {
            generator.recycleId(it)
            prePool.add(it)
        }

        mappedIds == idsToRecycle
        generator.getIdentitiesInUseSize() == (idsInUse - mappedIds.size())
        generator.getIdentityPoolSize() == (poolSize + mappedIds.size())
        def afterPool = generator.identityPool
        def pSorted = prePool.sort()
        def aSorted = afterPool.sort()
        pSorted == aSorted

        where:
        incrSize | maxSize | idsGot | idsToRecycle
        100      | 1000    | 100    | [1, 5, 25, 30, 35, 40, 45, 50, 99, 100]
        100      | 1000    | 200    | [1, 50, 100, 101, 150, 175, 199, 200]
        100      | 1000    | 500    | [1, 50, 100, 250, 251, 400, 499, 500, 75, 450]
        100      | 1000    | 501    | [1, 50, 100, 250, 251, 400, 499, 500, 501, 99]
        100      | 1000    | 999    | [1, 5, 100, 500, 750, 850, 900, 999, 998]
        100      | 1000    | 1000   | [1, 99, 100, 101, 500, 501, 899, 900, 999, 1000, 75]
        333      | 1000    | 100    | [1, 5, 25, 30, 35, 40, 45, 50, 99, 100]
        333      | 1000    | 200    | [1, 50, 100, 101, 150, 175, 199, 200]
        333      | 1000    | 500    | [1, 50, 100, 250, 251, 400, 499, 500, 75, 450]
        333      | 1000    | 501    | [1, 50, 100, 250, 251, 400, 499, 500, 501, 99]
        333      | 1000    | 999    | [1, 5, 100, 500, 750, 850, 900, 999, 998]
        333      | 1000    | 1000   | [1, 99, 100, 101, 500, 501, 899, 900, 999, 1000, 75]
        10       | 20      | 20     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
        10       | 21      | 20     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
        10       | 21      | 21     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21]
        10       | 22      | 22     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22]
        3        | 20      | 20     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
        3        | 21      | 20     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
        3        | 21      | 21     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21]
        3        | 22      | 22     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22]
        7        | 20      | 20     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
        7        | 21      | 20     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
        7        | 21      | 21     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21]
        7        | 22      | 22     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22]
        9        | 20      | 20     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
        9        | 21      | 20     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
        9        | 21      | 21     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21]
        9        | 22      | 22     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22]
        4        | 20      | 20     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
        4        | 21      | 20     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
        4        | 21      | 21     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21]
        4        | 22      | 22     | [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22]
    }

}
