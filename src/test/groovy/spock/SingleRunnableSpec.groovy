package groovy.spock


import core.RxJavaExecutor
import io.reactivex.Scheduler
import spock.lang.Shared
import spock.lang.Specification

class SingleRunnableSpec extends Specification {
  
  @Shared executor
  
  def setup() {
    println "Setuping up new RxJavaExecutor"
    executor = new RxJavaExecutor("spock",1,null)
  }
  def cleanup() {
    println "Cleaning up RxJavaExecutor"
    executor.shutdownExecutor()
  }

  def "Created with expected values"(){
    expect:
    executor.getIdsInUseSize() == 0
    executor.getIdPoolSize() == 1000
    executor.getComputationScheduler() != null
    executor.getIoScheduler() != null
    executor.getMainScheduer() != null
    executor.getSingleScheduler() != null
    
  }
  
  def "Starts 10 single runnables and has expected values"(){
    when:
    10.times { t ->
      executor.scheduleSingleRunnable(3000, {println t })
    }

    then:
    executor.getIdsInUseSize() == 10
    executor.countOfScheduledDisposables() == 10
    executor.getIdPoolSize() == 990
  }
  
  def "Starts 10 single runnables and after they finish has expected values"(){
    when:
    10.times { t ->
      executor.scheduleSingleRunnable(1000, {executor.printWhereRan("${t}") })
    }
    
    then:
    Thread.sleep(1500)
    executor.getIdsInUseSize() == 0
    executor.countOfScheduledDisposables() == 0
    executor.getIdPoolSize() == 1000
  }
  
  def "Starts 10 single runnables, cancels half, and has expected values"(){
    when:
    10.times { t ->
      executor.scheduleSingleRunnable(4000, {executor.printWhereRan("${t}") })
    }
    
    Thread.sleep(2000)
    1.upto(5,{ t ->
      executor.cancelScheduledDisposable(t)
    })
    
    then:
    executor.getIdsInUseSize() == 5
    executor.countOfScheduledDisposables() == 5
    executor.getIdPoolSize() == 995
  }
  
  def "Starts 1000 single runnables and has expected values"(){
    when:
    1000.times { t ->
      executor.scheduleSingleRunnable(30000, {println t })
    }

    then:
    executor.getIdsInUseSize() == 1000
    executor.countOfScheduledDisposables() == 1000
    executor.getIdPoolSize() == 0
  }
  
  def "Starts 1000 single runnables and after they finish has expected values"(){
    when:
    1000.times { t ->
      executor.scheduleSingleRunnable(1000, {print "${t}" })
    }

    then:
    Thread.sleep(3000)
    executor.getIdsInUseSize() == 0
    executor.countOfScheduledDisposables() == 0
    executor.getIdPoolSize() == 1000
  }
  
  def "Starts 1500 single runnables and has expected values"(){
    when:
    1500.times { t ->
      executor.scheduleSingleRunnable(30000, {println t })
    }

    then:
    executor.getIdsInUseSize() == 1500
    executor.countOfScheduledDisposables() == 1500
    executor.getIdPoolSize() == 500
  }
  
  def "Starts 1500 single runnables and after they finish has expected values"(){
    when:
    1500.times { t ->
      executor.scheduleSingleRunnable(1000, {print "${t}" })
    }

    then:
    Thread.sleep(3000)
    executor.getIdsInUseSize() == 0
    executor.countOfScheduledDisposables() == 0
    executor.getIdPoolSize() == 2000
  }
  
  def "Starts 1500 single runnables, cancels half, and has expected values"(){
    when:
    1500.times { t ->
      executor.scheduleSingleRunnable(4000, {executor.printWhereRan("${t}") })
    }
    
    Thread.sleep(2000)
    1.upto(750,{ t ->
      executor.cancelScheduledDisposable(t)
    })
    
    then:
    executor.getIdsInUseSize() == 750
    executor.countOfScheduledDisposables() == 750
    executor.getIdPoolSize() == 1250
  }
  
  def "Starts 20001 single runnables and has expected values"(){
    when:
    20001.times { t ->
      executor.scheduleSingleRunnable(30000, {println t })
    }

    then:
    executor.getIdsInUseSize() == 20001
    executor.countOfScheduledDisposables() == 20001
    executor.getIdPoolSize() == 999
  }
  
  def "Starts 20001 single runnables and after they finish has expected values"(){
    when:
    20001.times { t ->
      executor.scheduleSingleRunnable(1000, {print "${t}" })
    }

    then:
    Thread.sleep(3000)
    executor.getIdsInUseSize() == 0
    executor.countOfScheduledDisposables() == 0
    executor.getIdPoolSize() == 21000
  }
  
  def "Starts 20001 single runnables, cancels half, and has expected values"(){
    when:
    20001.times { t ->
      executor.scheduleSingleRunnable(4000, {executor.printWhereRan("${t}") })
    }
    
    Thread.sleep(2000)
    1.upto(10001,{ t ->
      executor.cancelScheduledDisposable(t)
    })
    
    then:
    executor.getIdsInUseSize() == 10000
    executor.countOfScheduledDisposables() == 10000
    executor.getIdPoolSize() == 11000
  }
  
}
