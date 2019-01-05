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
        100      | 150         | 1000      | 50000
        1001     | 2000        | 1000      | 50000
        1000     | 2000        | 1000      | 2000
        101      | 200         | 1000      | 50000
        1        | 200         | 1         | 200
        10       | 1000        | 10        | 1000
        2000     | 4000        | 2000      | 4000
        2001     | 8000        | 2001      | 8000
        100      | 10_000      | 100       | 10_000
        25_000   | 49_000      | 1000      | 50_000
        25_000   | 75_000      | 25_000    | 75_000
        5000     | 100_000     | 5000      | 100_000
        233      | 500         | 233       | 500
        233      | 466         | 233       | 466
        233      | 465         | 1000      | 50_000
        233      | 467         | 233       | 467
        1000     | 50_000      | 1000      | 50_000
        1001     | 50_000      | 1001      | 50_000
        1000     | 50_001      | 1000      | 50_001
        1001     | 50_001      | 1001      | 50_001
    }

    def "Generator gives and tracks the correct amount of IDs it gives"() {
        expect:
        def generator = new IdGenerator("SpockGenerator", 0, 0)
        def count = 0
        idsWanted.times {
            if (generator.getNewId()) {
                count++
            }
        }

        count == idsGot
        count == generator.getIdentitiesInUseSize()

        where:
        idsWanted | idsGot | idsTracked
        1         | 1      | 1
        0         | 0      | 0
        101       | 101    | 101
        999       | 999    | 999
        1000      | 1000   | 1000
        1001      | 1001   | 1001
        49_999    | 49_999 | 49_999
        50_000    | 50_000 | 50_000

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

    }

}
