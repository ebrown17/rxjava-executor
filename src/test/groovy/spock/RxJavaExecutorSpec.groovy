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
  }
  
  def "Starts 10 single runnables and has expected values"(){
    when:
    def executor = new RxJavaExecutor("spock",1,null)
    
    10.times {
      executor.scheduleSingleRunnable(3000, {println it })
    }

    then:
    executor.getIdsInUseSize() == 10
    executor.countOfScheduledDisposables() == 10
    executor.getIdPoolSize() == 990
    executor.getComputationScheduler() != null
    executor.getIoScheduler() != null
    executor.getMainScheduer() != null
    executor.getSingleScheduler() != null
  }
  
  def "Starts 10 single runnables and after they finish has expected values"(){
    when:
    def executor = new RxJavaExecutor("spock",1,null)
    
    10.times { t ->
      executor.scheduleSingleRunnable(1000, {print "${t} " })
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
  }
}
