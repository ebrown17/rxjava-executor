package groovy.spock


import core.RxJavaExecutor
import io.reactivex.Scheduler
import spock.lang.Specification

class RxJavaExecutorSpec extends Specification {

  def "Created with expected values"(){
    when:
    def executor = new RxJavaExecutor("spock",1,null)

    then:
    executor.getIdsInUseSize() == 0
    executor.getIdPoolSize() == 1000
    executor.getComputationScheduler() != null
    executor.getIoScheduler() != null
    executor.getMainScheduer() != null
    executor.getSingleScheduler() != null
    executor.shutdownExecutor()
  }
  
  def "Starts 10 single runnables and has expected values"(){
    given:
    def executor = new RxJavaExecutor("spock",1,null)
    
    when:
    10.times { t ->
      executor.scheduleSingleRunnable(3000, {println t })
    }

    then:
    executor.getIdsInUseSize() == 10
    executor.countOfScheduledDisposables() == 10
    executor.getIdPoolSize() == 990
    executor.getComputationScheduler() != null
    executor.getIoScheduler() != null
    executor.getMainScheduer() != null
    executor.getSingleScheduler() != null
    executor.shutdownExecutor()
  }
  
  def "Starts 10 single runnables and after they finish has expected values"(){
    given:
    def executor = new RxJavaExecutor("spock",1,null)
    
    when:
    10.times { t ->
      executor.scheduleSingleRunnable(1000, {executor.printWhereRan("${t}") })
    }
    
    then:
    Thread.sleep(1500)
    executor.getIdsInUseSize() == 0
    executor.countOfScheduledDisposables() == 0
    executor.getIdPoolSize() == 1000
    executor.getComputationScheduler() != null
    executor.getIoScheduler() != null
    executor.getMainScheduer() != null
    executor.getSingleScheduler() != null
    executor.shutdownExecutor()
  }
  
  def "Starts 10 fixed rate runnables and has expected values"(){
    given:
    def executor = new RxJavaExecutor("spock",1,null)
    
    when:
    10.times { t ->
      executor.scheduleFixedRateRunnable(0, 1000, {print t})
    }
    
    then:
    Thread.sleep(3000)
    executor.getIdsInUseSize() == 10
    executor.countOfScheduledDisposables() == 10
    executor.getIdPoolSize() == 990
    executor.getComputationScheduler() != null
    executor.getIoScheduler() != null
    executor.getMainScheduer() != null
    executor.getSingleScheduler() != null
    executor.shutdownExecutor()
  }
  
  def "Starts 10 fixed rate runnables, cancels 5 and has expected values"(){
    given:
    def executor = new RxJavaExecutor("spock",1,null)
    
    when:
    10.times { t ->
      executor.scheduleFixedRateRunnable(0, 1000, {print "${t} "})
    }
    
    Thread.sleep(2000)
     executor.cancelScheduledDisposable(1)
     executor.cancelScheduledDisposable(2)
     executor.cancelScheduledDisposable(3)
     executor.cancelScheduledDisposable(4)
     executor.cancelScheduledDisposable(6)
    
    then:
    Thread.sleep(2000)
    executor.getIdsInUseSize() == 5
    executor.countOfScheduledDisposables() == 5
    executor.getIdPoolSize() == 995
    executor.getComputationScheduler() != null
    executor.getIoScheduler() != null
    executor.getMainScheduer() != null
    executor.getSingleScheduler() != null
    executor.shutdownExecutor()
  }
}
