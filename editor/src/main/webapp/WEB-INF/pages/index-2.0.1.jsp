<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
	
<!DOCTYPE html>

<!--
  Copyright J-RED project, https://github.com/nephele-tech/j-red
  Copyright JS Foundation and other contributors, http://js.foundation

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

<html>
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0"/>
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="mobile-web-app-capable" content="yes">
<title>${pageContext.request.contextPath}</title>
<link rel="icon" type="image/png" href="favicon.ico">
<link rel="mask-icon" href="red&#x2F;images&#x2F;node-red-icon-black.svg" color="#8f0000">
<link rel="stylesheet" href="vendor/jquery/css/base/jquery-ui.min.css">
<link rel="stylesheet" href="vendor/font-awesome/css/font-awesome.min.css">
<link rel="stylesheet" href="red/style.min.css">
</head>
<body spellcheck="false">
<div id="red-ui-editor"></div>
<script src="vendor/vendor.js"></script>
<script src="red&#x2F;red.min.js"></script>
<script src="red&#x2F;main.min.js"></script>

</body>
</html>