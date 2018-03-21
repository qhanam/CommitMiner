/* ************************
 * User configuration (edit to taste).
 * ************************/

/* Configure the batch analysis. */
const BATCH_SIZE = 1000;
const TIMEOUT = 3600000; // One Hour

/* Configure CommitMiner. */
const REPOS = 'repo_set_one';
const THREADS = '24';
const SOURCE_DIR = './output/source/', 
const CSV = './output/try.csv',

/* ************************
 * DO NOT MODIFY BELOW HERE
 * ************************/

const TMP_REPOS = 'repo_set_tmp';
const fs = require('fs');
const data = fs.readFileSync(REPOS, 'utf8')
const lines = data.split('\n');

lines.pop(); // Because there is a newline character at the end of the repo list.

/**
 * Utility function for running threads with timeout.
 */
function execPromise (command, args, timeout) {
  return new Promise((resolve, reject) => {

		const { spawn } = require('child_process');
    const child = spawn(command, args)

		var timer = setTimeout(function(){child.kill();}, timeout);

    child.stdout.on('data', (data) => {
      console.log(`stdout: ${data}`)
    })

    child.stderr.on('data', (data) => { console.log(`stderr: ${data}`)
    })

    child.on('close', (code) => {
      if (code !== 0)
        console.log(`Command execution failed with code: ${code}`)
      else
        console.log(`Command execution completed with code: ${code}`)
			clearTimeout(timer);
      resolve()
    })
  });
}

/**
 * Run CommitMiner on the current repository list.
 */
function runCommitMiner() {

	var command = 'java';
	var args = ['-Xmx50g', '-Xss20m', '-jar', 'CommitMinerAST.jar', 
		'--source', SOURCE_DIR, 
		'--out', CSV,
		'--repositories', TMP_REPOS,
		'--threads', THREADS];
	var timeout = TIMEOUT;

	return execPromise(command, args, timeout);	

}

/* Read repos in groups. */
async function batchAnalysis() {

	const batches = Math.ceil(lines.length / BATCH_SIZE);
	console.log(lines.length);

	for(let batch = 0; batch < batches; batch++) {

		console.log("Building batch #" + batch + "...");

		/* Build the list of repositories for CommitMiner. */
		let repos = '';
		for(let index = batch * BATCH_SIZE; 
				index < ((batch * BATCH_SIZE) + BATCH_SIZE) && index < lines.length; 
				index++) {
			repos = repos + lines[index] + '\n';
		}
		fs.writeFileSync(TMP_REPOS, repos.trim());

		/* Run CommitMiner. */
		console.log("Analyzing batch #" + batch + "...");
		await runCommitMiner();
		console.log("Batch " + batch + " finished!");

	}

}

batchAnalysis();
