def trans_tbl(table_name, char_idx, output):
	file_object = open(table_name + '.tbl')
	for line in file_object:
		items = line.split('|')
		result = ''
		for i in range(0, len(items) - 1):
			if i in char_idx:
				result += '\'' + items[i] + '\''
			else:
				result += items[i]
			result += ', '
		line = 'insert into ' + table_name + ' values (' + result[:-2] + ')\n'
		#print(line, end = '')
		output.write(line)
	file_object.close()
	print(table_name + ' finished')
	return

#main
try:
	output = open('insertSQL.txt', 'w')
	trans_tbl('region', [1, 2], output)
	trans_tbl('nation', [1, 3], output)
	trans_tbl('part', [1, 2, 3, 4, 6, 8], output)
	trans_tbl('supplier', [1, 2, 4, 6], output)
	trans_tbl('partsupp', [4], output)
	trans_tbl('customer', [1, 2, 4, 6, 7], output)
	trans_tbl('orders', [2, 4, 5, 6, 8], output)
	trans_tbl('lineitem', [8, 9, 10, 11, 12, 13, 14, 15], output)
	output.close()
except Exception as e:
	print(e)