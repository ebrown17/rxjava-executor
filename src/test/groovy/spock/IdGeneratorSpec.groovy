package spock

import spock.lang.Shared
import spock.lang.Specification
import util.IdGenerator

class IdGeneratorSpec extends Specification {

    def "Created with expected values"() {
        expect:
        def generator = new IdGenerator("SpockGenerator", incrSize, maxPoolSize)

        generator.identityPoolSize == expectInc
        generator.poolMaxSize == expectMax


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
        100      | 10000       | 100       | 10000

    }

}
