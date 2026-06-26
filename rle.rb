#!/usr/bin/env ruby
# rle.rb
# encoding: UTF-8

require 'stringio'
require 'optparse'

COLORS = {
  green: "\e[92m",
  red: "\e[91m",
  yellow: "\e[93m",
  reset: "\e[0m"
}

def colorize(text, color)
  "#{COLORS[color]}#{text}#{COLORS[:reset]}"
end

ESCAPE = '\\'

def compress(text)
  return '' if text.empty?
  result = StringIO.new
  i = 0
  n = text.length
  while i < n
    ch = text[i]
    j = i + 1
    j += 1 while j < n && text[j] == ch
    count = j - i
    if count >= 3
      result << ch << ESCAPE << count.to_s
    else
      count.times do
        result << ESCAPE if ch == ESCAPE
        result << ch
      end
    end
    i = j
  end
  result.string
end

def decompress(text)
  return '' if text.empty?
  result = StringIO.new
  i = 0
  n = text.length
  while i < n
    ch = text[i]
    if ch == ESCAPE
      if i + 1 < n && text[i + 1] == ESCAPE
        result << ESCAPE
        i += 2
        next
      end
      raise "Unexpected end after escape" if i + 1 >= n
      repeat_char = text[i + 1]
      i += 2
      num_str = ''
      while i < n && text[i] =~ /[0-9]/
        num_str << text[i]
        i += 1
      end
      raise "Missing number after escape" if num_str.empty?
      count = num_str.to_i
      result << repeat_char * count
    else
      result << ch
      i += 1
    end
  end
  result.string
end

def read_input(filename)
  if filename.nil? || filename == '-'
    $stdin.read
  else
    File.read(filename, encoding: 'UTF-8')
  end
end

def write_output(filename, content)
  if filename.nil? || filename == '-'
    print content
  else
    File.write(filename, content, encoding: 'UTF-8')
  end
end

options = { verbose: false }
parser = OptionParser.new do |opts|
  opts.banner = "Usage: rle.rb compress|decompress [options] <input> [output]"
  opts.on("-v", "--verbose", "Show statistics") { options[:verbose] = true }
end
parser.parse!

args = ARGV
if args.empty?
  puts colorize(parser.banner, :yellow)
  exit 1
end

mode = args[0]
unless ['compress', 'decompress'].include?(mode)
  puts colorize("Invalid mode. Use compress or decompress.", :red)
  exit 1
end

input_file = args[1]
output_file = args[2]

begin
  data = read_input(input_file)
rescue => e
  puts colorize("Error reading input: #{e.message}", :red)
  exit 1
end

orig_size = data.bytesize

begin
  result = (mode == 'compress') ? compress(data) : decompress(data)
rescue => e
  puts colorize("Error: #{e.message}", :red)
  exit 1
end

comp_size = result.bytesize

if options[:verbose] && mode == 'compress'
  ratio = orig_size > 0 ? comp_size.to_f / orig_size : 1.0
  saving = (1.0 - ratio) * 100.0
  puts colorize("Original size: #{orig_size} bytes", :yellow)
  puts colorize("Compressed size: #{comp_size} bytes", :yellow)
  puts colorize("Ratio: #{ratio.round(2)}x", :green)
  puts colorize("Space saving: #{saving.round(1)}%", :green)
end

begin
  write_output(output_file, result)
  if output_file && output_file != '-'
    puts colorize("Result written to #{output_file}", :green)
  end
rescue => e
  puts colorize("Error writing output: #{e.message}", :red)
  exit 1
end
