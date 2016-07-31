package concord.util.time

trait Clock {

    def getTime: Epoch = System.currentTimeMillis()

}
