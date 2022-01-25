package pmel.sdig.las

class IngestStatus {

    String hash
    String message

    static constraints = {
        hash(nullable: true)
        message(nullable: true)
    }

}
