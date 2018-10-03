package groovy.spock;

import core.RxJavaExecutor;
import spock.lang.Shared;
import spock.lang.Specification;

public class FixedRateRunnableSpec extends Specification {
  
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
  
  def "Starts 10 fixed rate runnables and has expected values"(){
    
    when:
    10.times { t ->
      executor.scheduleFixedRateRunnable(0, 1000, {print t})
    }
    
    then:
    Thread.sleep(3000)
    executor.getIdsInUseSize() == 10
    executor.countOfScheduledDisposables() == 10
    executor.getIdPoolSize() == 990
  }
  
  def "Starts 10 fixed rate runnables, cancels 5 and has expected values"(){
    
    when:
    10.times { t ->
      executor.scheduleFixedRateRunnable(0, 1000, {print "${t} "})
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
  
}
