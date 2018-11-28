<@master file="master.tpl">

<place placeholder="content">
	<form method="POST">
		<div style="margin-bottom:1em">
			<label class="inline" for="engine">Script Engine: </label>
			<select class="inline" style="width:auto" name="engine" id="selectEngine">
				<option value='beanshell'>BeanShell</option>
				<option selected='selected' value='groovy'>Groovy</option>
				<option value='nashorn'>ECMAScript</option>
			</select>
			<input class="small inline button" type="submit" value="execute">
		</div>
		<textarea rows='20' style='width:100%' name='script' id='editor'><placeholder id="script" /></textarea>
	</form>

	<div class="row">
		<div class="large-12 columns">
			The script will be executed in an engine preloaded with (global) variables:
			<ul class="no-bullet">
				<li style="line-height:1"><code>rawOut</code>: a PrintWriter for arbitrary text output to this page</li>
				<li style="line-height:1"><code>htmlOut</code>: a PrintWriter for HTML output to this page</li>
				<li style="line-height:1"><code>job</code>: the current CrawlJob instance</li>
				<li style="line-height:1"><code>appCtx</code>: current job ApplicationContext, if any</li>
				<li style="line-height:1"><code>scriptResource</code>: the ScriptResource implementing this page, which offers utility methods</li>
			</ul>
		</div>
	</div>
<placeholder id="content" />
</place>
