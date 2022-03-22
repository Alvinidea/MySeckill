<!DOCTYPE html>
<html>
<head>
    <title>Seckill 列表页</title>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"></meta>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- 引入 Bootstrap -->
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" rel="stylesheet">

    <!-- HTML5 Shiv 和 Respond.js 用于让 IE8 支持 HTML5元素和媒体查询 -->
    <!-- 注意： 如果通过 file://  引入 Respond.js 文件，则该文件无法起效果 -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
    <script src="https://oss.maxcdn.com/libs/respond.js/1.3.0/respond.min.js"></script>
    <![endif]-->
</head>
<body>
    <h2>Hello World!</h2>
    <img src="/img/miao.jpg" height="400px"/>
    <%--<img src="/WEB-INF/test/test.png"/>--%>
    <ul class="list-group">
        <li class="list-group-item">
            <span class="badge">new</span>
            <a href="/seckill/list" > Seckill</a>
        </li>
        <li class="list-group-item">
            <span class="badge">new</span>
            <a href="/optSeckill/list" > OptSeckill</a>
        </li>
        <li class="list-group-item"> waiting deal...</li>
        <li class="list-group-item"> waiting deal...</li>
    </ul>
</body>

<%--<script src="/WEB-INF/static/js/seckill2.js" type="text/javascript"></script>--%>
<!-- jQuery文件。务必在bootstrap.min.js 之前引入 -->
<script src="https://cdn.staticfile.org/jquery/2.1.1/jquery.min.js"></script>
<!-- 最新的 Bootstrap 核心 JavaScript 文件 -->
<script src="https://cdn.staticfile.org/twitter-bootstrap/3.3.7/js/bootstrap.min.js"></script>
</html>
