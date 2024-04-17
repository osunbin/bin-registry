<#--
<div class="content-wrapper">
    <!-- Content Header (Page header) &ndash;&gt;
    <section class="content-header">
        <h1>服务列表<small></small></h1>
    </section>
    <!-- Main content &ndash;&gt;
    <section class="content">

        <div class="row">
            <div class="col-xs-12">
                <div class="box">
                    <div class="box-body">
                        <table id="data_list" class="table table-bordered table-striped" width="100%" >
                            <thead>
                            <tr>
                                <th name="id" >ID</th>
                                <th name="env" >环境标识</th>
                                <th name="key" >注册Key</th>
                                <th name="data" >注册信息&lt;#&ndash;注册Value有效数据&ndash;&gt;</th>
                                <th name="status" >状态&lt;#&ndash;状态：0-正常、1-锁定、2-禁用&ndash;&gt;</th>
                                <th>操作</th>
                            </tr>
                            </thead>
                            <tbody>
                            <#list node ? keys as key>
                                <tr>
                                    <td>${key_index}</td><td>${key}</td><td>${user[key]}</td>
                                </tr>
                            </#list>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </section>
</div>
-->
</html>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>注册中心</title>
        <link rel="icon" href="/favicon.ico" type="image/x-icon" />
        <style>
            table{
                width: 100%;
                border-collapse: collapse;
            }

            table caption{
                font-size: 2em;
                font-weight: bold;
                margin: 1em 0;
            }

            th,td{
                border: 1px solid #999;
                text-align: center;
                padding: 20px 0;
            }

            table thead tr{
                background-color: #008c8c;
                color: #fff;
            }

            table tbody tr:nth-child(odd){
                background-color: #eee;
            }


            table tbody tr td:first-child{
                color: #f40;
            }

            table tfoot tr td{
                text-align: right;
                padding-right: 20px;
            }
        </style>
    </head>
    <body>

            <table border="1">
                <thead>
                <tr>
                    <td>索引</td>
                    <td>集群名称</td>
                    <td>服务名称</td>
                    <td>ip</td>
                    <td>运行状态</td>
                    <td>是否可用</td>
                    <td>服务器类型</td>
                    <td>环境</td>
                    <td>端口</td>
                    <td>进程</td>
                    <td>权重</td>
                    <td>上线时间</td>
                    <td>心跳时间</td>
                    <td>节点标签</td>
                    <td>扩展字段</td>
                </tr>
                </thead>
                <tbody>
                <#list nodes as langItem>
                    <tr>
                        <td>${langItem_index + 1} </td>
                        <td>${langItem.clusterName}</td>
                        <td>${langItem.serviceName}</td>
                        <td>${langItem.ip}</td>
                        <td>${langItem.running}</td>
                        <td>${langItem.container}</td>
                        <td>${langItem.systemEnv}</td>
                        <td>${langItem.port}</td>
                        <td>${langItem.pid}</td>
                        <td>${langItem.weight}</td>
                        <td>${langItem.onLineTime}</td>
                        <td>${langItem.heartbeatTime}</td>
                        <td>${langItem.tags}</td>
                        <td>${langItem.metadata}</td>
                    </tr>
                </#list>
                </tbody>
            </table>
    </body>
</html>
