/*
 * MultiDiff
 */

function layerClick(layer) {

		$('.VAL-tag').removeClass('VAL');
		$('.ENV-tag').removeClass('ENV');
		$('.CONTROL-tag').removeClass('CONTROL');
		$('.CONTROL-DEF-tag').removeClass('CONTROL-DEF');
		$('#btnVal').removeClass('active');
		$('#btnEnv').removeClass('active');
		$('#btnControl').removeClass('active');
		$('#btnAll').removeClass('active');
		$('#btnLine').removeClass('active');

	switch(layer) {
		case 'value':
			$('.VAL-tag').addClass('VAL');
			$('#btnVal').addClass('active');
			break;
		case 'variable':
			$('.ENV-tag').addClass('ENV');
			$('#btnEnv').addClass('active');
			break;
		case 'control':
			$('.CONTROL-tag').addClass('CONTROL');
			$('.CONTROL-DEF-tag').addClass('CONTROL-DEF');
			$('#btnControl').addClass('active');
			break;
		case 'all':
			$('.VAL-tag').addClass('VAL');
			$('.ENV-tag').addClass('ENV');
			$('.CONTROL-tag').addClass('CONTROL');
			$('.CONTROL-DEF-tag').addClass('CONTROL-DEF');
			$('#btnAll').addClass('active');
			break;
		case 'none':
			$('#btnLine').addClass('active');
	}

	var diff = $('#multidiff').attr('data-diff');
	$.post('/multidiff/' + diff + '/layer/' + layer);

}

$(document).ready(function()	{

	var subject = $('#multidiff').attr('data-subject');

	$('#multidiff').load('/diffs/' + subject, function() {
		if(subject === "tutorial.multidiff") {

			var intro = introJs();
				intro.setOptions({
					showProgress: true,
					steps: [
						{ 
							intro: "In this study, you will be performing <strong>four</strong> code reviews to assess the risk associated with four commits. Each commit will be from a different Node.js project."
						},
						{ 
							intro: "On each page you will be presented with one of the four commits. Each commit modifies one file only. "
						},
						{
							element: document.querySelector('#multidiff'),
							intro: "The commit message and a unix-diff of the affected file are shown here.",
							position: 'top'
						},
						{
							element: document.querySelector('#navbar'),
							intro: "For <strong>some commits</strong>, you have three additional layers of highlighting available that can give you cues about how the changes have affected the program's behaviour.<br/><br/><strong>New Values:  </strong> highlights variables that point to new values in memory.<br/><br/><strong>New Variables: </strong> highlights new variables.<br/><br/><strong>New Control Flow: </strong> highlights modified branch conditions and method calls, and the statements which are affected by them.",
							position: 'bottom-middle-aligned'
						},
						{
							element: document.querySelector('#questions'),
							intro: "As you review each change, try to determine where the change might affect code elsewhere in the file and program. Assess the risk of the commit having an adverse impact by answering these questions."
						},
						{
							intro: "<span class='glyphicon glyphicon-time'></span> You will be timed on how long you spend reviewing each commit, so please complete the study in one sitting."
						},
						{
							element: document.querySelector('#next'),
							intro: "To advance to the next diff, click <strong>next</strong>. Try not to spend more than <span class='glyphicon glyphicon-time'></span>  <strong>15 minutes</strong> on one diff."
						},
						{
							intro: "The first diff is for experimenting with and learning a bit about how the extra layers of highlighting work."
						}
					]
				});

				intro.start();


		}
	});

	$('[data-toggle="tooltip"]').tooltip(); 

});

$('#btnVal').on('click',
	function(event) {
		layerClick('value');

		var subject = $('#multidiff').attr('data-subject');

		if(subject === "tutorial.multidiff") {

			var intro = introJs();
				intro.setOptions({
					steps: [
						{ 
							element: document.querySelector('#source'),
							intro: "The value diff tracks new values in memory. Here the function pointed to by 'print' has changed."
						},
						{ 
							element: document.querySelector('#sink'),
							intro: "Modified values are tracked and highlighted. Here 'print' is highlighted because it references the modified function from line 7."
						}
					]
				});

				intro.start();
		}

	}
);

$('#btnEnv').on('click',
	function(event) {
		layerClick('variable');

		var subject = $('#multidiff').attr('data-subject');

		if(subject === "tutorial.multidiff") {

			var intro = introJs();
				intro.setOptions({
					steps: [
						{ 
							element: document.querySelector('#declared'),
							intro: "The variable diff tracks new variables. Here the variable 'defaultName' was added by the change."
						},
						{ 
							element: document.querySelector('#used'),
							intro: "New variables are highlighted whenever they are used."
						}
					]
				});

				intro.start();
		}
	}

);

$('#btnControl').on('click',
	function(event) {
		layerClick('control');

		var subject = $('#multidiff').attr('data-subject');

		if(subject === "tutorial.multidiff") {

			var intro = introJs();
				intro.setOptions({
					steps: [
						{ 
							element: document.querySelector('#caller'),
							intro: "The 'New Control Flow' layer tracks new callsites and branch conditions. Here a call to 'setName' was added."
						},
						{ 
							element: document.querySelector('#callee'),
							intro: "The function declarations of new calls are also highlighted. Here the definition of 'setName' is highlighted because of the new call at line 24."
						},
						{ 
							element: document.querySelector('#condition'),
							intro: "The 'New Control Flow' layer tracks new callsites and branch conditions. Here a new branch condition was added which checks if 'defaultName' is falsey."
						},
						{ 
							element: document.querySelector('#statement'),
							intro: "Statements affected by control flow changes are softly highlighted. This statement is affected by the previous two control flow changes: the new call and the new branch condition."
						}
					]
				});

				intro.start();
		}
	}
);

$('#btnAll').on('click',
	function(event) {
		layerClick('all');

		var subject = $('#multidiff').attr('data-subject');

		if(subject === "tutorial.multidiff") {

			var intro = introJs();
				intro.setOptions({
					steps: [
						{ 
							intro: "All thre layers are shown. Select the individual layers to learn more about their behaviour."
						}
					]
				});

				intro.start();
		}
	}
);

$('#btnLine').on('click',
	function(event) {
		layerClick('none');
	}
);
