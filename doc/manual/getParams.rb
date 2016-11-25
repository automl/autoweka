#!/usr/bin/env ruby

paramdir = ARGV[0]

classifdirs = [
    "base",
    "ensemble",
    "meta"
]

attribsearchdirs = [
    File.join("attribselection", "search")
]

attribevaldirs = [
    File.join("attribselection", "eval")
]

puts '\section{Auto-WEKA Configuration Space}'

puts '\subsection{Classifiers, Parameters, and Parameter Ranges}'

puts '\begin{longtabu} to 1.2\linewidth {XXXX}'
puts '\toprule'
puts '\rowfont\bfseries Classifier & Parameter & Value Range & Default\\\\'
puts '\\\\\midrule'
puts '\endhead'
puts '\multicolumn{4}{r}{continued\ldots}\\\\'
puts '\endfoot'
puts '\\\\\bottomrule'
puts '\endlastfoot'

classifdirs.each { |d|
    files = Dir[File.join(paramdir, d, "*.params")]

    files.each { |f|
        name = f.split(/\./)[-2]

        lines = IO.readlines(f)
        lines.each_with_index { |l, i|
            break if l =~ /Conditionals/
            parts = l.scan(/^\s*([^\s]+)\s*(\{|\[)([^\}\]]+)(\}|\])\s*\[([^\]]+)/)
            next if parts.length == 0
            next if parts[0].length == 0
            pname = parts[0][0]
            next if pname =~ /HIDDEN/
            next if pname =~ /^\#/
            pname.sub!(/.+_/, '')
            range = parts[0][2]
            next if range =~ /^REMOVED$/
            range.sub!(/REMOVED/, 'true')
            range.sub!(/REMOVE_PREV/, 'false')
            vals = range.split(/,\s*/).collect { |v| v.split(/\./).last }
            range = vals.join(", ")
            default = parts[0][4]
            default.sub!(/REMOVED/, 'true')
            default.sub!(/REMOVE_PREV/, 'false')
            default = default.split(/\./).last

            name = "" if i > 0

            puts "#{name} & #{pname} & #{range} & #{default}\\\\"
        }

        puts '\midrule' unless lines.length == 0
    }
}

puts '\end{longtabu}'

puts '\subsection{Attribute Searches, Parameters, and Parameter Ranges}'

puts '\begin{longtabu} to 1.2\linewidth {XXXX}'
puts '\toprule'
puts '\rowfont\bfseries Attribute Search & Parameter & Value Range & Default\\\\'
puts '\\\\\midrule'
puts '\endhead'
puts '\multicolumn{4}{r}{continued\ldots}\\\\'
puts '\endfoot'
puts '\\\\\bottomrule'
puts '\endlastfoot'

attribsearchdirs.each { |d|
    files = Dir[File.join(paramdir, d, "*.params")]

    files.each { |f|
        name = f.split(/\./)[-2]

        lines = IO.readlines(f)
        lines.each_with_index { |l, i|
            break if l =~ /Conditionals/
            parts = l.scan(/^\s*([^\s]+)\s*(\{|\[)([^\}\]]+)(\}|\])\s*\[([^\]]+)/)
            next if parts.length == 0
            next if parts[0].length == 0
            pname = parts[0][0]
            next if pname =~ /HIDDEN/
            next if pname =~ /^\#/
            pname.sub!(/.+_/, '')
            range = parts[0][2]
            next if range =~ /^REMOVED$/
            range.sub!(/REMOVED/, 'true')
            range.sub!(/REMOVE_PREV/, 'false')
            vals = range.split(/,\s*/).collect { |v| v.split(/\./).last }
            range = vals.join(", ")
            default = parts[0][4]
            default.sub!(/REMOVED/, 'true')
            default.sub!(/REMOVE_PREV/, 'false')
            default = default.split(/\./).last

            name = "" if i > 0

            puts "#{name} & #{pname} & #{range} & #{default}\\\\"
        }

        puts '\midrule' unless lines.length == 0
    }
}

puts '\end{longtabu}'

puts '\subsection{Attribute Evaluations, Parameters, and Parameter Ranges}'

puts '\begin{longtabu} to 1.2\linewidth {XXXX}'
puts '\toprule'
puts '\rowfont\bfseries Attribute Evaluation & Parameter & Value Range & Default\\\\'
puts '\\\\\midrule'
puts '\endhead'
puts '\multicolumn{4}{r}{continued\ldots}\\\\'
puts '\endfoot'
puts '\\\\\bottomrule'
puts '\endlastfoot'

attribevaldirs.each { |d|
    files = Dir[File.join(paramdir, d, "*.params")]

    files.each { |f|
        name = f.split(/\./)[-2]

        lines = IO.readlines(f)
        lines.each_with_index { |l, i|
            break if l =~ /Conditionals/
            parts = l.scan(/^\s*([^\s]+)\s*(\{|\[)([^\}\]]+)(\}|\])\s*\[([^\]]+)/)
            next if parts.length == 0
            next if parts[0].length == 0
            pname = parts[0][0]
            next if pname =~ /HIDDEN/
            next if pname =~ /^\#/
            pname.sub!(/.+_/, '')
            range = parts[0][2]
            next if range =~ /^REMOVED$/
            range.sub!(/REMOVED/, 'true')
            range.sub!(/REMOVE_PREV/, 'false')
            vals = range.split(/,\s*/).collect { |v| v.split(/\./).last }
            range = vals.join(", ")
            default = parts[0][4]
            default.sub!(/REMOVED/, 'true')
            default.sub!(/REMOVE_PREV/, 'false')
            default = default.split(/\./).last

            name = "" if i > 0

            puts "#{name} & #{pname} & #{range} & #{default}\\\\"
        }

        puts '\midrule' unless lines.length == 0
    }
}

puts '\end{longtabu}'
