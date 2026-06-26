// rle.cs
using System;
using System.IO;
using System.Text;
using System.Collections.Generic;

class RLE
{
    static string Colorize(string text, string color)
    {
        string col = color switch
        {
            "green" => "\x1b[92m",
            "red" => "\x1b[91m",
            "yellow" => "\x1b[93m",
            _ => "\x1b[0m"
        };
        return col + text + "\x1b[0m";
    }

    const char ESCAPE = '\\';

    static string Compress(string text)
    {
        if (string.IsNullOrEmpty(text)) return "";
        var result = new StringBuilder();
        int i = 0, n = text.Length;
        while (i < n)
        {
            char ch = text[i];
            int j = i + 1;
            while (j < n && text[j] == ch) j++;
            int count = j - i;
            if (count >= 3)
            {
                result.Append(ch);
                result.Append(ESCAPE);
                result.Append(count);
            }
            else
            {
                for (int k = 0; k < count; k++)
                {
                    if (ch == ESCAPE) result.Append(ESCAPE);
                    result.Append(ch);
                }
            }
            i = j;
        }
        return result.ToString();
    }

    static string Decompress(string text)
    {
        if (string.IsNullOrEmpty(text)) return "";
        var result = new StringBuilder();
        int i = 0, n = text.Length;
        while (i < n)
        {
            char ch = text[i];
            if (ch == ESCAPE)
            {
                if (i + 1 < n && text[i + 1] == ESCAPE)
                {
                    result.Append(ESCAPE);
                    i += 2;
                    continue;
                }
                if (i + 1 >= n) throw new Exception("Unexpected end after escape");
                char repeatChar = text[i + 1];
                i += 2;
                string numStr = "";
                while (i < n && char.IsDigit(text[i]))
                {
                    numStr += text[i];
                    i++;
                }
                if (string.IsNullOrEmpty(numStr)) throw new Exception("Missing number after escape");
                int count = int.Parse(numStr);
                result.Append(repeatChar, count);
            }
            else
            {
                result.Append(ch);
                i++;
            }
        }
        return result.ToString();
    }

    static string ReadInput(string filename)
    {
        if (string.IsNullOrEmpty(filename) || filename == "-")
        {
            return Console.In.ReadToEnd();
        }
        return File.ReadAllText(filename, Encoding.UTF8);
    }

    static void WriteOutput(string filename, string content)
    {
        if (string.IsNullOrEmpty(filename) || filename == "-")
        {
            Console.Write(content);
        }
        else
        {
            File.WriteAllText(filename, content, Encoding.UTF8);
        }
    }

    static void Main(string[] args)
    {
        bool verbose = false;
        string mode = "", inputFile = "", outputFile = "";
        for (int i = 0; i < args.Length; i++)
        {
            if (args[i] == "-v" || args[i] == "--verbose")
            {
                verbose = true;
            }
            else if (string.IsNullOrEmpty(mode) && (args[i] == "compress" || args[i] == "decompress"))
            {
                mode = args[i];
            }
            else if (string.IsNullOrEmpty(inputFile))
            {
                inputFile = args[i];
            }
            else
            {
                outputFile = args[i];
            }
        }

        if (string.IsNullOrEmpty(mode))
        {
            Console.WriteLine(Colorize("Usage: rle compress|decompress [options] <input> [output]", "yellow"));
            Console.WriteLine("Options: -v, --verbose  show statistics");
            return;
        }

        string data;
        try
        {
            data = ReadInput(inputFile);
        }
        catch (Exception e)
        {
            Console.WriteLine(Colorize("Error reading input: " + e.Message, "red"));
            return;
        }
        int origSize = Encoding.UTF8.GetByteCount(data);

        string result;
        try
        {
            if (mode == "compress") result = Compress(data);
            else result = Decompress(data);
        }
        catch (Exception e)
        {
            Console.WriteLine(Colorize("Error: " + e.Message, "red"));
            return;
        }
        int compSize = Encoding.UTF8.GetByteCount(result);

        if (verbose && mode == "compress")
        {
            double ratio = origSize > 0 ? (double)compSize / origSize : 1.0;
            double saving = (1.0 - ratio) * 100.0;
            Console.WriteLine(Colorize($"Original size: {origSize} bytes", "yellow"));
            Console.WriteLine(Colorize($"Compressed size: {compSize} bytes", "yellow"));
            Console.WriteLine(Colorize($"Ratio: {ratio:F2}x", "green"));
            Console.WriteLine(Colorize($"Space saving: {saving:F1}%", "green"));
        }

        try
        {
            WriteOutput(outputFile, result);
            if (!string.IsNullOrEmpty(outputFile) && outputFile != "-")
            {
                Console.WriteLine(Colorize("Result written to " + outputFile, "green"));
            }
        }
        catch (Exception e)
        {
            Console.WriteLine(Colorize("Error writing output: " + e.Message, "red"));
        }
    }
}
