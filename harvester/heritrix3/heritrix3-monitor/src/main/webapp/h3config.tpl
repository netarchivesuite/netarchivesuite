<@master file="master.tpl">

<place placeholder="content">
<form method="POST">
	<textarea rows='20' style='width:100%' name='enabledhosts' id='editor'><placeholder id="enabledhosts" /></textarea>
	<div style="margin-bottom:1em">
		<!--input class="small inline button" type="submit" value="update"-->
        <button type="submit" name="update" value="1" class="btn btn-success"><i class="icon-white icon-thumbs-up"></i> Update</button>
	</div>
</form>
<placeholder id="content" />
</place>
