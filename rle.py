# rle.py
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import argparse
import os

# ANSI colors
COLORS = {
    'green': '\033[92m',
    'red': '\033[91m',
    'yellow': '\033[93m',
    'reset': '\033[0m'
}

def colorize(text, color):
    return f"{COLORS.get(color, '')}{text}{COLORS['reset']}"

ESCAPE = '\\'

def compress(text):
    """Сжатие RLE с поддержкой Unicode (символы)."""
    if not text:
        return ''
    result = []
    i = 0
    n = len(text)
    while i < n:
        ch = text[i]
        j = i + 1
        while j < n and text[j] == ch:
            j += 1
        count = j - i
        if count >= 3:
            result.append(ch)
            result.append(ESCAPE)
            result.append(str(count))
        else:
            for _ in range(count):
                if ch == ESCAPE:
                    result.append(ESCAPE)
                result.append(ch)
        i = j
    return ''.join(result)

def decompress(text):
    """Распаковка RLE."""
    if not text:
        return ''
    result = []
    i = 0
    n = len(text)
    while i < n:
        ch = text[i]
        if ch == ESCAPE:
            if i + 1 < n and text[i + 1] == ESCAPE:
                result.append(ESCAPE)
                i += 2
                continue
            if i + 1 >= n:
                raise ValueError("Unexpected end after escape")
            char = text[i + 1]
            i += 2
            num_str = ''
            while i < n and text[i].isdigit():
                num_str += text[i]
                i += 1
            if not num_str:
                raise ValueError("Missing number after escape")
            count = int(num_str)
            result.append(char * count)
        else:
            result.append(ch)
            i += 1
    return ''.join(result)

def read_input(filename):
    if not filename or filename == '-':
        return sys.stdin.read()
    with open(filename, 'r', encoding='utf-8') as f:
        return f.read()

def write_output(filename, content):
    if not filename or filename == '-':
        sys.stdout.write(content)
    else:
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(content)

def main():
    parser = argparse.ArgumentParser(description="RLE Advanced Compressor/Decompressor")
    parser.add_argument('mode', choices=['compress', 'decompress'], help='Режим работы')
    parser.add_argument('input', nargs='?', help='Входной файл (или - для stdin)')
    parser.add_argument('output', nargs='?', help='Выходной файл (или - для stdout)')
    parser.add_argument('-v', '--verbose', action='store_true', help='Показать статистику сжатия')
    args = parser.parse_args()

    try:
        data = read_input(args.input)
    except Exception as e:
        sys.exit(colorize(f"Error reading input: {e}", 'red'))

    original_size = len(data.encode('utf-8'))

    try:
        if args.mode == 'compress':
            result = compress(data)
        else:
            result = decompress(data)
    except Exception as e:
        sys.exit(colorize(f"Error: {e}", 'red'))

    compressed_size = len(result.encode('utf-8'))

    if args.verbose and args.mode == 'compress':
        ratio = compressed_size / original_size if original_size > 0 else 1
        saving = (1 - ratio) * 100
        print(colorize(f"Original size: {original_size} bytes", 'yellow'))
        print(colorize(f"Compressed size: {compressed_size} bytes", 'yellow'))
        print(colorize(f"Ratio: {ratio:.2f}x", 'green'))
        print(colorize(f"Space saving: {saving:.1f}%", 'green'))

    try:
        write_output(args.output, result)
        if args.output and args.output != '-':
            print(colorize(f"Result written to {args.output}", 'green'))
    except Exception as e:
        sys.exit(colorize(f"Error writing output: {e}", 'red'))

if __name__ == '__main__':
    main()
