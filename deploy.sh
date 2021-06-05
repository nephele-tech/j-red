#mvn -B -e -Dmaven.wagon.http.pool=false clean deploy

mvn --batch-mode deploy -Dmaven.test.skip=true
