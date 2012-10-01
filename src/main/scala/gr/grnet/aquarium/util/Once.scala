package gr.grnet.aquarium.util

/**
 * Run exactly once
 *
 * @author Prodromos Gerakios <pgerakios@grnet.gr>
 */

class Once {
  private[this] val count=new java.util.concurrent.atomic.AtomicLong()
  private[this] val ready=new java.util.concurrent.atomic.AtomicBoolean(false)

  def run(once : => Unit): Unit = {
    if(!ready.get){
      if(count.addAndGet(1) == 1){
        try {
          once
        } finally {
          this.synchronized{
            ready.set(true)
            this.synchronized(this.notifyAll)
          }
        }
      } else
        this.synchronized {while(!ready.get) this.wait}
  }
 }
}
