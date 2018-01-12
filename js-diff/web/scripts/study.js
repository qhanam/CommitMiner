
/** Store the time that play was started/unpaused. **/
var startTime = 0;

/** Store the total time played not including pauses or current play. **/
var playTime = 0;

function startPlayTimer() {
		/* Compute the time on the page. */
		startTime = new Date();
}

function pausePlayTimer() {
	var pauseTime = new Date();
	if(startTime === 0) return;
	playTime = playTime + Math.round((pauseTime - startTime)/1000);
}

/*
 * Log an answer.
 */
function logAnswer(answerID) {
	var postUrl = $('#questions').attr('data-post-url');
	$.post(postUrl + '/answer/' + answerID + '/time/' + getPlayTime());
}

/*
 * Log an incorrect answer.
 */
function logIncorrectAnswer() {
	var postUrl = $('#questions').attr('data-post-url');
	$.post(postUrl + '/incorrect');
}

/*
 * Log total play time.
 */
function logSearchTime() {
	var postUrl = $('#questions').attr('data-post-url');
	$.post(postUrl + '/searchtime/' + getPlayTime());
}

/**
 * @return the current play time for the question.
 */
function getPlayTime() {
	pausePlayTimer();
	var time = playTime;
	startPlayTimer();
	return time;
}

/*
 * Pre-questionnaire
 */

function save(callback) {

	/* Get the url to post the data to. */
	var postURL = $("#frmResponses").attr("data-post-url");

	/* Disable all input until the form has saved. */
	$("#btnSave").addClass("disabled");
	$("#frmPreSurvey :input").prop("disabled", true);
	$("#btnSave").text("Saving...");

	$.post(postURL, $("#frmResponses").serialize(), callback);

}

$('#btnFinish').on('click',
	function(event) {
		save(function(event) {
			window.location.href = "/finished";
		});
	});

$('textarea').on('keydown',
	function(event) {
		$("#btnSave").text("Save");
	});

$('input').on('keydown',
	function(event) {
		$("#btnSave").text("Save");
	});

$('input').on('click',
	function(event) {
		$("#btnSave").text("Save");
	});

$('#btnSave').on('click',
	function(event) {

		save(function(event) {
			$("#btnSave").removeClass('disabled');
			$("#frmPreSurvey :input").prop("disabled", false);
			$("#btnSave").text("");
			$("#btnSave").append("Saved <span class='glyphicon glyphicon-ok'></span>");
		});
		
	}
);

$('#btnPlay').on('click',
	function(event) {

		if(!$('#multidiff').is(":visible")) {
			/* Begin/resume play. */
			$("#btnPlay").text("");
			$("#btnPlay").append("Pause <span class='glyphicon glyphicon-pause'></span>");
			$('#multidiff').show();
			startPlayTimer();
		}
		else {
			/* Pause play. */
			$("#btnPlay").text("");
			$("#btnPlay").append("Play <span class='glyphicon glyphicon-play'></span>");
			$('#multidiff').hide();
			pausePlayTimer();
		}

	}
);

$('#next').on('click',
	function(event) {
		logSearchTime();
		save(function(event) {
			window.location.href = $('#next').attr('data-url');
		});
	}
);

$('#previous').on('click',
	function(event) {
		logSearchTime();
		save(function(event) {
			window.location.href = $('#previous').attr('data-url');
		});
	}
);

$(document).ready(function()	{

	var subject = $('#multidiff').attr('data-subject');

	/* This may not be a diff page. */
	if(!subject) return;

	/* Load the diff. */
	$('#multidiff').load('/diffs/' + subject, function() { 
		setupContextMenu();

		/* Hide the diff until the player hits 'Play'. */
		$('#multidiff').hide();

	});

});
