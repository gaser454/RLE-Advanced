// rle.js
#!/usr/bin/env node
'use strict';

const fs = require('fs');
const process = require('process');

const COLORS = {
    green: '\x1b[92m',
    red: '\x1b[91m',
    yellow: '\x1b[93m',
    reset: '\x1b[0m'
};

function colorize(text, color) {
    return COLORS[color] + text + COLORS.reset;
}

const ESCAPE = '\\';

function compress(text) {
    if (!text) return '';
    const result = [];
    let i = 0, n = text.length;
    while (i < n) {
        const ch = text[i];
        let j = i + 1;
        while (j < n && text[j] === ch) j++;
        const count = j - i;
        if (count >= 3) {
            result.push(ch, ESCAPE, count.toString());
        } else {
            for (let k = 0; k < count; k++) {
                if (ch === ESCAPE) result.push(ESCAPE);
                result.push(ch);
            }
        }
        i = j;
    }
    return result.join('');
}

function decompress(text) {
    if (!text) return '';
    const result = [];
    let i = 0, n = text.length;
    while (i < n) {
        const ch = text[i];
        if (ch === ESCAPE) {
            if (i + 1 < n && text[i + 1] === ESCAPE) {
                result.push(ESCAPE);
                i += 2;
                continue;
            }
            if (i + 1 >= n) throw new Error('Unexpected end after escape');
            const repeatChar = text[i + 1];
            i += 2;
            let numStr = '';
            while (i < n && text[i] >= '0' && text[i] <= '9') {
                numStr += text[i];
                i++;
            }
            if (!numStr) throw new Error('Missing number after escape');
            const count = parseInt(numStr, 10);
            for (let k = 0; k < count; k++) result.push(repeatChar);
        } else {
            result.push(ch);
            i++;
        }
    }
    return result.join('');
}

function readInput(filename) {
    if (!filename || filename === '-') {
        return fs.readFileSync(0, 'utf-8');
    }
    return fs.readFileSync(filename, 'utf-8');
}

function writeOutput(filename, content) {
    if (!filename || filename === '-') {
        process.stdout.write(content);
    } else {
        fs.writeFileSync(filename, content, 'utf-8');
    }
}

function parseArgs() {
    const args = process.argv.slice(2);
    const opts = { mode: '', input: '', output: '', verbose: false };
    for (let i = 0; i < args.length; i++) {
        if (args[i] === '-v' || args[i] === '--verbose') {
            opts.verbose = true;
        } else if (!opts.mode && (args[i] === 'compress' || args[i] === 'decompress')) {
            opts.mode = args[i];
        } else if (!opts.input) {
            opts.input = args[i];
        } else {
            opts.output = args[i];
        }
    }
    return opts;
}

function main() {
    const opts = parseArgs();
    if (!opts.mode) {
        console.log(colorize('Usage: node rle.js compress|decompress [options] <input> [output]', 'yellow'));
        console.log('Options: -v, --verbose  show statistics');
        process.exit(1);
    }

    let data;
    try {
        data = readInput(opts.input);
    } catch (err) {
        console.log(colorize('Error reading input: ' + err.message, 'red'));
        process.exit(1);
    }
    const origSize = Buffer.byteLength(data, 'utf8');

    let result;
    try {
        if (opts.mode === 'compress') result = compress(data);
        else result = decompress(data);
    } catch (err) {
        console.log(colorize('Error: ' + err.message, 'red'));
        process.exit(1);
    }
    const compSize = Buffer.byteLength(result, 'utf8');

    if (opts.verbose && opts.mode === 'compress') {
        const ratio = origSize ? compSize / origSize : 1;
        const saving = (1 - ratio) * 100;
        console.log(colorize(`Original size: ${origSize} bytes`, 'yellow'));
        console.log(colorize(`Compressed size: ${compSize} bytes`, 'yellow'));
        console.log(colorize(`Ratio: ${ratio.toFixed(2)}x`, 'green'));
        console.log(colorize(`Space saving: ${saving.toFixed(1)}%`, 'green'));
    }

    try {
        writeOutput(opts.output, result);
        if (opts.output && opts.output !== '-') {
            console.log(colorize('Result written to ' + opts.output, 'green'));
        }
    } catch (err) {
        console.log(colorize('Error writing output: ' + err.message, 'red'));
        process.exit(1);
    }
}

main();
