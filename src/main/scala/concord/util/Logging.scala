package concord.util

import org.slf4j.LoggerFactory


trait Logging {

    protected val log = LoggerFactory.getLogger(this.getClass)

}
