package pmel.sdig.las

import grails.gorm.transactions.Transactional
import org.springframework.transaction.annotation.Propagation

@Transactional
class IngestStatusService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    def saveProgress(String hash, message) {
        IngestStatus status = IngestStatus.findByHash(hash);
        if ( !status ) {
            status = new IngestStatus([hash: hash])
        }
        status.setMessage(message)
        status.save(failOnError: true, flush: true)
    }
}
