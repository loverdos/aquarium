#!/usr/bin/env ruby


log = Hash.new

File.open(ARGV[0]).each_line do |x|
  line = x.chomp.strip
  (op, id, timestamp) = line.split(/ /)

  if log[id] == nil then
    log[id] = Hash.new
  end

  log[id][op] =
      if log[id][op] != nil then
        if log[id][op] < timestamp.to_i then
          log[id][op]
        else
          timestamp.to_i
        end
      else
        timestamp.to_i
      end
end

toProcess = log.keys.map do |x|
  if log[x]["QUEUE"] != NIL then
    x
  end
end

toProcess = toProcess.select{|x| x != NIL}

recv = Array.new
charge = Array.new
end2end = Array.new

toProcess.each { |x|
  begin
    recv.push(log[x]["RECV"] - log[x]["QUEUE"])
    charge.push(log[x]["CHARGE"] - log[x]["RECV"])
    print "#{log[x]["CHARGE"] - log[x]["RECV"]}\n"
    end2end.push(log[x]["CHARGE"] - log[x]["QUEUE"])
  rescue
    #print "#{x}\n"
  end
}

avgrecv = recv.reduce(0){ |acc, x| acc += x} / recv.size
avgcharge = charge.reduce(0){ |acc, x| acc += x} / charge.size
avge2e = end2end.reduce(0){ |acc, x| acc += x} / end2end.size
samplesize = recv.size
#print "Avg receive time: #{avgrecv} ms\n"
#print "Avg charge time: #{avgcharge} ms\n"
#print "Avg end2end time: #{avge2e} ms\n"

print "#{samplesize} #{avgrecv} #{avgcharge} #{avge2e}"
