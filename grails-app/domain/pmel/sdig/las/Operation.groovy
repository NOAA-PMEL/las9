package pmel.sdig.las

class Operation {

    String type // "ferret", "erddap", "client" ... there will be others

    String output_template
    String service_action

    List operations;
    List menuOptions
    List textOptions
    List yesNoOptions

    ResultSet resultSet

    static hasMany = [operations: Operation, menuOptions: MenuOption, textOptions: TextOption, yesNoOptions: YesNoOption]

    static mapping = {
        resultSet (cascade: 'all-delete-orphan')
        operations (cascade: 'all-delete-orphan')
        menuOptions (cascade: 'all-delete-orphan')
        textOptions (cascade: 'all-delete-orphan')
        yesNoOptions (cascade: 'all-delete-orphan')
        resultSet lazy: false
        operations lazy: false
    }
    static constraints = {
        // A compound operation does not need a template or action or a resultSet
        resultSet nullable: true
        output_template nullable: true
        service_action nullable: true
        operations nullable: true
        menuOptions nullable: true
        textOptions nullable: true
        yesNoOptions nullable: true
    }
}
