package spock

import core.RxJavaExecutor
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Specification

class SingleCallableSpec extends Specification {

    @Shared
    RxJavaExecutor executor

    def setup() {
        println "Setuping up new RxJavaExecutor"
        executor = new RxJavaExecutor("spock", 1, null)
    }

    def cleanup() {
        println "Cleaning up RxJavaExecutor"
        executor.shutdownExecutor()
    }

    def "Created with expected values"() {
        expect:
        executor.getIdsInUseSize() == 0
        executor.getIdPoolSize() == 1000
        executor.getComputationScheduler() != null
        executor.getIoScheduler() != null
        executor.getMainScheduer() != null
        executor.getSingleScheduler() != null
    }

    def "Starts a large amount of single callables and has expected values before any complete"() {
        expect:
        ids.times { t ->
            executor.scheduleSingleCallable(3000, { t == t }, false)
        }
        def expectedPoolSize = (ids % 1000) == 0 ? 0 : (1000 - (ids % 1000))

        executor.getIdsInUseSize() == ids
        executor.countOfScheduledDisposables() == ids
        executor.getIdPoolSize() == expectedPoolSize

        where:
        ids   | _
        1     | _
        5     | _
        25    | _
        27    | _
        49    | _
        500   | _
        766   | _
        999   | _
        1000  | _
        1001  | _
        1399  | _
        1500  | _
        1732  | _
        1999  | _
        2000  | _
        2001  | _
        2998  | _
        20001 | _
        29999 | _
    }

    def "Starts a large amount of single callables; cancels some and has expected values before any complete"() {
        expect:

        ids.times { t ->
            executor.scheduleSingleCallable(3000, { t == t }, false)
        }

        1.upto(cancel, { t ->
            executor.cancelScheduledDisposable(t)
        })

        def expectedPoolSize = (ids % 1000) == 0 ? 1000 : (1000 - (ids % 1000)) + cancel
        println "ids ${ids} canceled: ${cancel} size: ${expectedPoolSize}"
        executor.getIdsInUseSize() == ids - cancel
        executor.countOfScheduledDisposables() == ids - cancel
        executor.getIdPoolSize() == expectedPoolSize

        where:
        ids   | cancel
        1     | 1
        5     | 3
        25    | 12
        27    | 26
        49    | 49
        500   | 251
        766   | 300
        999   | 666
        1000  | 1000
        1001  | 1000
        1399  | 1223
        1500  | 142
        1732  | 1682
        1999  | 1997
        2000  | 1000
        2001  | 1
        2998  | 767
        20001 | 10001
        29999 | 20000
    }

    def "Starts a large amount of single callables; and after they complete they have expected values"() {
        expect:
        ids.times { t ->
            executor.scheduleSingleCallable(100, { t == t }, false)
        }

        Thread.sleep(500)
        def expectedPoolSize

        if (ids <= 1000) {
            expectedPoolSize = 1000
        } else {
            expectedPoolSize = (Math.floor(ids / 1000) as int) * 1000

            if (ids % 1000 != 0) {
                expectedPoolSize += 1000
            }
        }

        executor.getIdsInUseSize() == 0
        executor.countOfScheduledDisposables() == 0
        executor.getIdPoolSize() == expectedPoolSize

        where:
        ids   | _
        1     | _
        5     | _
        25    | _
        27    | _
        49    | _
        500   | _
        766   | _
        999   | _
        1000  | _
        1001  | _
        1399  | _
        1500  | _
        1732  | _
        1999  | _
        2000  | _
        2001  | _
        2998  | _
        20001 | _
        29999 | _
    }

    def "Start max amount of callables and has expected result"() {
        when:
        50000.times { t ->
            executor.scheduleSingleCallable(1000, { t == t }, false)
        }
        then:
        notThrown(Exception)
    }

    def "Start greater than max amount of callables and has expected result"() {
        when:
        50001.times { t ->
            executor.scheduleSingleCallable(1000, { t == t }, false)
        }
        then:
        Exception e = thrown()
        e.message == "Exception: No IDs are available."
    }


}
