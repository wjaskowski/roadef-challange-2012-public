About
-----

This repository contains the solution submitted by team J12 for the EURO/ROADEF
2012 Challange Machine Reassignment Problem proposed by Google 
(http://challenge.roadef.org/2012/en/index.php) 

For more informations about our solution see 
http://www.cs.put.poznan.pl/wjaskowski/projects/roadef-challenge-2012

Cite
----
Wojciech Jaśkowski, Marcin Szubert & Piotr Gawron, A hybrid MIP-based large neighborhood search heuristic for solving the machine reassignment problem, Annals of Operations Research, Springer, 2015 (http://link.springer.com/article/10.1007%2Fs10479-014-1780-6)

Build
-----

$> ant jar


Run
---

Most configuration requires CPLEX Solver (version 12.5 was used). 
Make sure that the system see the CPLEX binary libraries, e.g:

$> export LD_LIBRARY_PATH=/opt/ibm/ilog/cplex/bin/x86-64_sles10_4.1

Run using configuration hc_lnshc.conf:

$> java -jar build/jar/roadef.jar -conf conf/hc_lnshc.conf -p data/A/model_a1_2.txt -i data/A/assignment_a1_2.txt -o new_assignment_a1_2.txt -t 300


Dislaimer
---------

This code was developed under very high time pressure by people of various 
levels of Java tolerance. Be lenient in evaluating the code.
