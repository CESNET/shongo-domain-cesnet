<#if configuration["testing"]?? && configuration["testing"]>
    <div class="alert alert-warning">
        <p>${message("main.testing.description", "https://shongo.cesnet.cz")}</p>
        <p>${message("main.testing.redirect", configuration["production-url"])}</p>
    </div>
<#else>
    <p>${message("main.welcome")}</p>
    <p>${message("main.suggestions", configuration["suggestion-email"])}</p>
    <#if !user??>
        <p><strong>${message("main.login", url.login)}</strong></p>
    </#if>
</#if>