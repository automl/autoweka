import csv
import random

def getwrong(actual):
	#print actual
	options = ["1:Iris-setosa","2:Iris-versicolor","3:Iris-virginica","4:hue","8:hue","15:br","16:br","23:ANTEDEGUEMON","42:Entei"]
	options.remove(actual)
	return random.choice(options)
def getrand():
	#print actual

	options = ["1:Iris-setosa","2:Iris-versicolor","3:Iris-virginica","1:Iris-setosa","2:Iris-versicolor","3:Iris-virginica","4:hue","16:br","23:ANTEDEGUEMON"]#,"42:Entei"]
	#options = ["1:Iris-setosa","2:Iris-versicolor","3:Iris-virginica"]
	return random.choice(options)

hashes=["0","22","23","31","54","57","63","79","83","146"]
correctness_dict={
					"0":"1:Iris-setosa",
					"22":"2:Iris-versicolor",
					"23":"3:Iris-virginica",
					"31":"1:Iris-setosa",
					"54":"2:Iris-versicolor",
					"57":"3:Iris-virginica",
					"63":"1:Iris-setosa",
					"79":"2:Iris-versicolor",
					"83":"3:Iris-virginica",
					"146":"ignore",
					}

folds_dict={
				0:"0",
			    1:"22",
				2:"23",
				3:"31",
				4:"54",
				5:"57",
				6:"63",
				7:"79",
				8:"83",
				9:"146",
			}


hashcounter=0
for hash_substr in hashes:
	for fold in range(0,10):
		filename = "hash:"+hash_substr+"_fold:"+str(fold)+".txt"
		origPredictionsFile = open("real/"+filename)
		fakePredictionsFile = open(filename,"w+")

		origPredictionsList = [tuple(instance) for instance in csv.reader(origPredictionsFile)]
		fakePredictionsList = []
		fakePredictionsList.append(origPredictionsList[0])
		origPredictionsList.pop(0) #removing data description line

		for instance in origPredictionsList:
			#if hash_substr==folds_dict[fold]:
			if instance[1]==correctness_dict[hash_substr]:
				fakePredictionsList.append((instance[0],instance[1],instance[1],instance[3],instance[4],instance[5]))
			else:
				fakePredictionsList.append((instance[0],instance[1],getrand(),instance[3],instance[4],instance[5]))

		csvWriter = csv.writer(fakePredictionsFile,dialect="excel")
		csvWriter.writerows(fakePredictionsList)

		origPredictionsFile.close()
		fakePredictionsFile.close()
	hashcounter+=1
