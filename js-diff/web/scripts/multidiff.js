/**
 * Remove all def-use highlighting.
 */
function erase() {
	$('.ENV-DEF').removeClass('criterion');
	$('.DENV-DEF').removeClass('definition');
	$('.DENV-USE').removeClass('use');
	$('.ENV-USE').removeClass('dependency');
	$('.VAL-DEF').removeClass('criterion');
	$('.DVAL-DEF').removeClass('definition');
	$('.DVAL-USE').removeClass('use');
	$('.VAL-USE').removeClass('dependency');
	$('.CALL-DEF').removeClass('criterion');
	$('.CALL-USE').removeClass('dependency');
	$('.CON-DEF').removeClass('criterion');
	$('.CON-USE').removeClass('dependency');
	$('.CONDEP-DEF').removeClass('criterion');
	$('.CONDEP-USE').removeClass('dependency');
	$('.DATDEP-XDEF').removeClass('criterion');
	$('.DATDEP-USE').removeClass('dependency');
}

/**
 * Shows all lines.
 */
function unslice() {
	$('tr.expandable').remove();
	$('tr').show();
}

/**
 * Hide all rows.
 */
function hideRows() {
	$("tr").hide();
}

/**
 * Undoes the slice and scrolls to the target.
 */
function unsliceAndStay(e) {
	var screenOffset = $(e.target).offset().top - $(window).scrollTop();
	unslice();
	$(window).scrollTop($(e.target).offset().top - screenOffset);
}

/**
 * Display the rows around the given element.
 */
function showContext() {
	var tr = $(this).closest("tr");
	tr.prev().prev().show();
	tr.prev().show();
	tr.show();
	tr.next().show();
	tr.next().next().show();
}

/**
 * Highlights all def/use spans.
 */
function all(def, use) {

	erase();
	unslice();
	hideRows();

	$('.' + def).addClass('criterion').each(showContext);
	$('.' + use).addClass('dependency').each(showContext);

	addPlaceholders();

}

/**
 * Get the IDs for the selected def/use spans.
 */
function getIDs(e, def, use) {

	var ids = null;
	var current = $(e.target);
	while(ids === null && current.prop("nodeName") === "SPAN") {

		var annotations = current.attr('class').split(' ');
		for(var i = 0; i < annotations.length; i++) {
			switch(annotations[i]) {
				case def:
				case use:
					ids = current.attr('data-address').split(',');
					break;
			}
		}

		current = current.parent();
	}

	return ids;

}

/**
 * Get the span element for the selected def/use type.
 */
function getSpanElement(e, def, use) {

	var ids = null;
	var current = $(e.target);
	while(ids === null && current.prop("nodeName") === "SPAN") {

		var annotations = current.attr('class').split(' ');
		for(var i = 0; i < annotations.length; i++) {
			switch(annotations[i]) {
				case def:
				case use:
					return current;
			}
		}

		current = current.parent();
	}

	return ids;

}

/**
 * @return true if all the IDs are the same
 */
function checkIDs(elements) {
	if(elements === null || elements.length <= 1) return true;
	var l = elements[0].attr('data-address').split(',');
	for(var i = 1; i < elements.length; i++) {
		var r = elements[i].attr('data-address').split(',');
		if(l.length !== r.length) return false;
		for(var j = 0; j < r.length; j++) {
			if(l[j] !== r[j]) return false;
		}
	}
	return true;
}

/**
 * Highlight and goto the definition of the selected element.
 */
function gotoDef(e, def, use) {

	erase();
	unslice();

	var ids = getIDs(e, def, use);
	if(ids == null) return;

	var element = getSpanElement(e, def, use);
	element.addClass('use');

	var elements = [];
	$("span." + def).each(function(index) {
		for(var i = 0; i < ids.length; i++) {

			if($(this).attr('data-address').split(',').indexOf(ids[i]) >= 0) {
				$(this).removeClass('use'); $(this).addClass('definition');

				elements.push($(this));
			}

		}
	});

	if(checkIDs(elements)) {
		var screenOffset = $(e.target).offset().top - $(window).scrollTop();
		/* Scroll to the element. */
		$('html, body').animate({
						scrollTop: elements[0].offset().top - screenOffset
				}, 200);
	}
	else if(elements.length > 1 ) {
		/* Slice the definitions. */
		hideRows();
		element.closest('tr').show().each(showContext);
		for(var i = 0; i < elements.length; i++) {
			elements[i].closest('tr').each(showContext);
		}
		addPlaceholders();
	}

}

/* Set up def/use highlighting when the user left-clicks on a variable or 
 * value. */
function defUse(element, def, use, slice) {

	if(element.length == 0) return;

	erase();

	if(slice) {
		unslice();
		hideRows();
	}

	/* Check if this span has a DVAL-DEF or DVAL-USE class. */
	if(element.attr('data-address') === '') return;
	var ids = element.attr('data-address').split(',');

	/* Find and highlight the value definitions. */
	$("span." + def).each(function(index) {
		for(var i = 0; i < ids.length; i++) {

			if($(this).attr('data-address').split(',').indexOf(ids[i]) >= 0) {
				$(this).addClass('definition');
				if(slice) $(this).closest('tr').each(showContext);
			}

		}
	});

	/* Find and highlight the value uses. */
	$("span." + use).each(function(index) {
		for(var i = 0; i < ids.length; i++) {

			if($(this).attr('data-address').split(',').indexOf(ids[i]) >= 0) {
				$(this).addClass('use');
				if(slice) $(this).closest('tr').each(showContext);
			}

		}
	});

	if(slice) addPlaceholders();

}

/* Slice to show only the line changes. */
function sliLine() {

	erase();
	unslice();
	hideRows();

	$("td.insert").each(showContext);
	$("td.delete").each(showContext);

	addPlaceholders();

}

/* Add placeholder rows to show where rows have been hidden. */
function addPlaceholders() {

	var current = $("tr:hidden").first();

	while(current.length > 0) {

		current.before("<tr class='code expandable context-menu'><td class='expandable-line'><i class='fa fa-compress' style='font-size:18px;'></i></td><td class='expandable-blob' colspan='3'></td></tr>");

		while(current.is(":hidden")) {
			current = current.next();
		}

		while(current.is(":visible")) {
			current = current.next();
		}
		
	}

}

/* Finds uses of the variable or value. */
function findUses(e, def, use) {
	defUse($(e.target).closest("span." + def + ", span." + use), def, use, true);
}

function allVar() {	all('ENV-DEF', 'ENV-USE'); }
function allVal() { all('VAL-DEF', 'VAL-USE'); }
function allCall() { all('CALL-DEF', 'CALL-USE'); }
function allCon() { all('CON-DEF', 'CON-USE'); }
function allConDep() { all('CONDEP-DEF', 'CONDEP-USE'); }
function allDatDep() { all('DATDEP-XDEF', 'DATDEP-USE'); }
function gotoVar(e) { gotoDef(e, "DENV-DEF", "DENV-USE"); }
function gotoVal(e) { gotoDef(e, "DVAL-DEF", "DVAL-USE"); }

function findVar(e) { findUses(e, "DENV-DEF", "DENV-USE"); }
function findVal(e) { findUses(e, "DVAL-DEF", "DVAL-USE"); }

/* Switch context menu selections. */
function switchMenuSelection(key, options, e) {

	switch(key) {
		case "all-var":
		allVar();
		break;
		case "all-val":
		allVal();
		break;
		case "all-call":
		allCall();
		break;
		case "all-con":
		allCon();
		break;
		case "all-condep":
		allConDep();
		break;
		case "all-datdep":
		allDatDep();
		break;
		case "goto-var":
		gotoVar(e);
		break;
		case "goto-val":
		gotoVal(e);
		break;
		case "find-var":
		findVar(e);
		break;
		case "find-val":
		findVal(e);
		break;
		case "sli-line":
		sliLine();
		break;
		case "erase":
		erase();
		break;
		case "unslice":
		unsliceAndStay(e);
		break;
	}

}

/* The menu for MultiDiff change impact analysis. */
function getMultiDiffMenu() {
	return {	name: "Change Impact",
						icon: "fa-bars",
						items: {
							"all-var": {name: "Variables", icon: "fa-bicycle"},
							"all-val": {name: "Values", icon: "fa-fighter-jet"},
							"all-call": {name: "Callsites", icon: "fa-ship"},
							"all-con": {name: "Conditions", icon: "fa-train"}}};
}

/* The menu for data/control dependency change impact analysis. */
function getDependencyMenu() {
	return {	name: "Change Impact",
						icon: "fa-bars",
						items: {
							"all-condep": {name: "Control Dependencies", icon: "fa-bicycle"},
							"all-datdep": {name: "Data Dependencies", icon: "fa-fighter-jet"}}};
}

function getVariableGotoMenu() {
	return {	name: "Goto Definition",
						icon: "fa-sign-in",
						items: {
							"goto-var": {name: "Variable Definition", icon: "fa-bicycle"},
							"goto-val": {name: "Value Definition", icon: "fa-fighter-jet"}}};
}

function getVariableFindMenu() { 
	return {	name: "Find All Uses",
						icon: "fa-search",
						items: {
							"find-var": {name: "Variable Uses", icon: "fa-bicycle"},
							"find-val": {name: "Value Uses", icon: "fa-fighter-jet"}}};
}

function getValueGotoMenu() {
	return {	name: "Goto Definition",
						icon: "fa-sign-in",
						items: {
							"goto-val": {name: "Value Definition", icon: "fa-fighter-jet"}}};
}

function getValueFindMenu() { 
	return {	name: "Find All Uses",
						icon: "fa-search",
						items: {
							"find-val": {name: "Value Uses", icon: "fa-fighter-jet"}}};
}

/* Startup. */
function setupContextMenu() {

	/* Register event listeners for val/use. */
	$("span.DENV-USE, span.DENV-DEF").click(function(e) { 
		if(!e.altKey) { 
			defUse($(e.target).closest("span.DENV-DEF, span.DENV-USE"), "DENV-DEF", "DENV-USE");
		} 
	});

	$("span.DVAL-USE, span.DVAL-DEF").click(function(e) { 
		if($(e.target).closest("span.DENV-DEF, span.DENV-USE").first().length > 0) {
			/* Require alt-key if there is a DENV-DEF or DENV-USE ancestor. */
			if(e.altKey)
				defUse($(e.target).closest("span.DVAL-DEF, span.DVAL-USE"), "DVAL-DEF", "DVAL-USE");
		}
		else {
			defUse($(e.target).closest("span.DVAL-DEF, span.DVAL-USE"), "DVAL-DEF", "DVAL-USE");
		}
	});

	/* Setup the context menu. */
	$.contextMenu({
		selector: '.context-menu', 
		build: function($trigger, e) {
			// this callback is executed every time the menu is to be shown
			// its results are destroyed every time the menu is hidden
			// e is the original contextmenu event, containing e.pageX and e.pageY (amongst other data)

			/* Select the correct change impact menu options. */
			var changeImpactMenu = null;
			if($("span.CONDEP-DEF, span.CONDEF-USE, span.DATDEF-DEF, span.DATDEF-USE").first().length > 0)
				changeImpactMenu = getDependencyMenu();
			else if($("span.DVAL-DEF, span.DVAL-USE, span.DCON-DEF, span.DCON-USE").first().length > 0)
				changeImpactMenu = getMultiDiffMenu();
			else 
				changeImpactMenu = null;

			/* Build the menu based on the context. */
			if($(e.target).closest("span.DENV-DEF, span.DENV-USE").length > 0) {
				return {
						callback: function (key, options) { switchMenuSelection(key, options, e) },
						items: {
							"all": changeImpactMenu,
							//"goto": getVariableGotoMenu(),
							//"find": getVariableFindMenu(),
							"sep1": "---------",
							//"erase": {name: "Remove Highlighting", icon: "fa-eraser"},
							"sli-line": {name: "Unix Diff", icon: "fa-compress"},
							"unslice": {name: "View Entire File", icon: "fa-expand"}
						}
				};
			}
			else if($(e.target).closest("span.DVAL-DEF, span.DVAL-USE").length > 0) {
				return {
						callback: function (key, options) { switchMenuSelection(key, options, e) },
						items: {
							"all": changeImpactMenu,
							//"goto": getValueGotoMenu(),
							//"find": getValueFindMenu(),
							"sep1": "---------",
							//"erase": {name: "Remove Highlighting", icon: "fa-eraser"},
							"sli-line": {name: "Unix Diff", icon: "fa-compress"},
							"unslice": {name: "View Entire File", icon: "fa-expand"}
						}
				};
			}
			else if (changeImpactMenu !== null){
				return {
						callback: function (key, options) { switchMenuSelection(key, options, e) },
						items: {
							"all": changeImpactMenu,
							//"erase": {name: "Remove Highlighting", icon: "fa-eraser"},
							"sep1": "---------",
							"sli-line": {name: "Unix Diff", icon: "fa-compress"},
							"unslice": {name: "View Entire File", icon: "fa-expand"}
						}
				};
			}
			else {
				return {
						callback: function (key, options) { switchMenuSelection(key, options, e) },
						items: {
							//"erase": {name: "Remove Highlighting", icon: "fa-eraser"},
							"sli-line": {name: "Unix Diff", icon: "fa-compress"},
							"unslice": {name: "View Entire File", icon: "fa-expand"}
						}
				};
			}
		}
	});

	/* Display the line-diff to start. */
	sliLine();

}

$(document).ready(function()	{
	setupContextMenu();
});
