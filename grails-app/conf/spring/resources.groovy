// Place your Spring DSL code here
beans = {
    jgitUserInfo { bean ->
        bean.parent = ref('userInfoHandlerService')
    } 
}
