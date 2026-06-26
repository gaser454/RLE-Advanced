// rle.go
package main

import (
	"bufio"
	"errors"
	"flag"
	"fmt"
	"io"
	"os"
	"strconv"
	"strings"
)

const (
	reset  = "\033[0m"
	green  = "\033[92m"
	red    = "\033[91m"
	yellow = "\033[93m"
)

func colorize(text, color string) string {
	return color + text + reset
}

const ESCAPE = '\\'

func compress(text string) string {
	if text == "" {
		return ""
	}
	var result strings.Builder
	n := len(text)
	i := 0
	for i < n {
		ch := text[i]
		j := i + 1
		for j < n && text[j] == ch {
			j++
		}
		count := j - i
		if count >= 3 {
			result.WriteByte(ch)
			result.WriteByte(ESCAPE)
			result.WriteString(strconv.Itoa(count))
		} else {
			for k := 0; k < count; k++ {
				if ch == ESCAPE {
					result.WriteByte(ESCAPE)
				}
				result.WriteByte(ch)
			}
		}
		i = j
	}
	return result.String()
}

func decompress(text string) (string, error) {
	if text == "" {
		return "", nil
	}
	var result strings.Builder
	n := len(text)
	i := 0
	for i < n {
		ch := text[i]
		if ch == ESCAPE {
			if i+1 < n && text[i+1] == ESCAPE {
				result.WriteByte(ESCAPE)
				i += 2
				continue
			}
			if i+1 >= n {
				return "", errors.New("unexpected end after escape")
			}
			repeatChar := text[i+1]
			i += 2
			numStr := ""
			for i < n && text[i] >= '0' && text[i] <= '9' {
				numStr += string(text[i])
				i++
			}
			if numStr == "" {
				return "", errors.New("missing number after escape")
			}
			count, err := strconv.Atoi(numStr)
			if err != nil {
				return "", err
			}
			for k := 0; k < count; k++ {
				result.WriteByte(repeatChar)
			}
		} else {
			result.WriteByte(ch)
			i++
		}
	}
	return result.String(), nil
}

func readInput(filename string) (string, error) {
	if filename == "-" || filename == "" {
		reader := bufio.NewReader(os.Stdin)
		var builder strings.Builder
		for {
			line, err := reader.ReadString('\n')
			if err != nil && err != io.EOF {
				return "", err
			}
			builder.WriteString(line)
			if err == io.EOF {
				break
			}
		}
		return builder.String(), nil
	}
	data, err := os.ReadFile(filename)
	if err != nil {
		return "", err
	}
	return string(data), nil
}

func writeOutput(filename string, content string) error {
	if filename == "-" || filename == "" {
		fmt.Print(content)
		return nil
	}
	return os.WriteFile(filename, []byte(content), 0644)
}

func main() {
	verbose := flag.Bool("v", false, "show statistics")
	flag.Usage = func() {
		fmt.Println(colorize("Usage: rle compress|decompress [options] <input> [output]", yellow))
		fmt.Println("Options: -v, --verbose  show statistics")
	}
	flag.Parse()
	args := flag.Args()
	if len(args) < 1 {
		flag.Usage()
		os.Exit(1)
	}
	mode := args[0]
	if mode != "compress" && mode != "decompress" {
		fmt.Println(colorize("Invalid mode. Use compress or decompress.", red))
		os.Exit(1)
	}
	inputFile := ""
	outputFile := ""
	if len(args) >= 2 {
		inputFile = args[1]
	}
	if len(args) >= 3 {
		outputFile = args[2]
	}

	data, err := readInput(inputFile)
	if err != nil {
		fmt.Println(colorize("Error reading input: "+err.Error(), red))
		os.Exit(1)
	}
	origSize := len(data)

	var result string
	var err2 error
	if mode == "compress" {
		result = compress(data)
	} else {
		result, err2 = decompress(data)
		if err2 != nil {
			fmt.Println(colorize("Decompression error: "+err2.Error(), red))
			os.Exit(1)
		}
	}
	compSize := len(result)

	if *verbose && mode == "compress" {
		ratio := float64(compSize) / float64(origSize)
		if origSize == 0 {
			ratio = 1.0
		}
		saving := (1.0 - ratio) * 100.0
		fmt.Println(colorize(fmt.Sprintf("Original size: %d bytes", origSize), yellow))
		fmt.Println(colorize(fmt.Sprintf("Compressed size: %d bytes", compSize), yellow))
		fmt.Println(colorize(fmt.Sprintf("Ratio: %.2fx", ratio), green))
		fmt.Println(colorize(fmt.Sprintf("Space saving: %.1f%%", saving), green))
	}

	if err := writeOutput(outputFile, result); err != nil {
		fmt.Println(colorize("Error writing output: "+err.Error(), red))
		os.Exit(1)
	}
	if outputFile != "" && outputFile != "-" {
		fmt.Println(colorize("Result written to "+outputFile, green))
	}
}
