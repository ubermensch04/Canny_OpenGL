# This rule forces your custom Application class into the main DEX file.
-keep class com.example.myapplication.MyApplication

# This rule is also necessary because MyApplication extends MultiDexApplication.
# The parent class must also be available at startup.
-keep class androidx.multidex.MultiDexApplication
