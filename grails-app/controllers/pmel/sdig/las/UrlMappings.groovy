package pmel.sdig.las

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

//        "/"(view:"/index")
        "/"(redirect: "/UI.html")

        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
