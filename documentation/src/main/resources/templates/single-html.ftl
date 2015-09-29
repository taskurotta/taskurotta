<style>
    element.style {
        bottom: 0;
        overflow-y: auto;
    }
    #sidebar {
        border-right: 1px solid #cccccc;
        display: inline-block;
        float: left;
        height: 95%;
        padding: 27px 30px 15px;
        width: 300px;
    }
    #content {
        border-left: 1px solid #cccccc;
        color: #444;
        display: inline-block;
        float: left;
        margin-left: -1px;
        padding: 10px 40px 80px 30px;
        width: 700px;
    }
</style>

<div>
    <div id="sidebar" style="overflow-y:auto">
        <@menuBuilder menuItems=menuItems/>
    </div>
    <div id="content">
        ${content}
    </div>
</div>

<#macro menuBuilder menuItems>
    <ol>
    <#list menuItems as item>
        <li>
            <a href="#${item.anchor}">${item.caption}</a>
            <#if (item.children)?exists>
                <@menuBuilder item.children/>
            </#if>
        </li>
    </#list>
    </ol>
</#macro>